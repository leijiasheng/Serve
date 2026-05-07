//package com.student.server.service.impl;
//
//import com.student.server.dao.ProductDAO;
//import com.student.server.dataobject.ProductDO;
//import com.student.server.model.Product;
//import com.student.server.model.Result;
//import com.student.server.service.SnappedService;
//import lombok.extern.slf4j.Slf4j;
//import org.redisson.api.RLock;
//import org.redisson.api.RedissonClient;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.util.concurrent.TimeUnit;
//
//@Service
//@Slf4j
//public class SnappedUpServiceImpl implements SnappedService {
//
//    @Autowired
//    private ProductDAO productDAO;
//
//    @Autowired
//    private RedisTemplate redisTemplate;
//
//    @Autowired
//    private RedissonClient redissonClient;
//
//    private static final String PRODUCT_STOCK_PREFIX = "product:stock:";
//
//    private static final String USER_SNAPPED_PREFIX = "user:snapped:";
//
//    private static final long SNAPPED_EXPIRE_DAY = 1;
//
//    private static final String LUA_SCRIPT =
//            "local stockKey = KEYS[1]\n" +
//                    "local userKey = KEYS[2]\n" +
//                    "if redis.call('exists', userKey) == 1 then return -2 end\n" +
//                    "local stock = tonumber(redis.call('get', stockKey) or 0)\n" +
//                    "if stock <= 0 then return -1 end\n" +
//                    "redis.call('decr', stockKey)\n" +
//                    "redis.call('set', userKey, 1, 'EX', 86400)\n" +
//                    "return 1";
//
//    @Override
//    public Result<Boolean> snappedUp(long productId, long userId) {
//        Result<Boolean> result = new Result<>();
//        result.setSuccess(true);
//
//        if (productId < 0) {
//            result.setSuccess(false);
//            result.setMessage("商品id错误");
//            return result;
//        }
//
//        String stockKey = PRODUCT_STOCK_PREFIX + productId;
//        String userSnappedKey = USER_SNAPPED_PREFIX + userId + ":" + productId;
//
//        //判断是否抢购过，一个人只能抢购一次
//        Boolean hasSnapped = redisTemplate.hasKey(userSnappedKey);
//        if (Boolean.TRUE.equals(hasSnapped)) {
//            result.setSuccess(false);
//            result.setMessage("您已抢购过该商品，每人仅限一次");
//            return result;
//        }
//
//        Integer finalStock =(Integer) redisTemplate.opsForValue().get(stockKey);
//        if (finalStock == null) {
//            ProductDO productDO = productDAO.selectById(productId);
//            if (productDO == null || productDO.getId() < 0) {
//                result.setSuccess(false);
//                result.setMessage("商品不存在");
//                return result;
//            }
//            finalStock = productDO.getStock();
//            redisTemplate.opsForValue().set(stockKey, finalStock);
//        }
//
//        String redissonKey = "productId-" + productId + "-lock";
//        RLock rLock = redissonClient.getLock(redissonKey);
//
//        try {
//            rLock.lock();
//            //扣减redis库存
//            Long remainStock = redisTemplate.opsForValue().increment(stockKey, -1);
//
//            if (remainStock < 0) {
//                redisTemplate.opsForValue().increment(stockKey, 1);
//                result.setSuccess(false);
//                result.setMessage("商品已被抢光了");
//                return result;
//            }
//
//            redisTemplate.opsForValue().set(userSnappedKey, 1, SNAPPED_EXPIRE_DAY, TimeUnit.DAYS);
//
//            int reduceRes = productDAO.reduceStock(productId, 1);
//            if (reduceRes <= 0) {
//                redisTemplate.opsForValue().increment(stockKey, 1);
//                redisTemplate.delete(userSnappedKey);
//                result.setSuccess(false);
//                result.setMessage("抢购失败，请稍后重试");
//                return result;
//            }
//        } catch (Exception e) {
//            log.error("some error .", e);
//        } finally {
//            rLock.unlock();
//        }
//
//        log.info("抢购成功，用户id：" + userId);
//        result.setMessage("库存扣减成功");
//        result.setData(true);
//        return result;
//    }
//
//
//}

package com.student.server.service.impl;

import com.alibaba.fastjson2.JSON;
import com.student.server.dao.OrderDAO;
import com.student.server.dao.ProductDAO;
import com.student.server.dao.SnappedUserDAO;
import com.student.server.dataobject.OrderDO;
import com.student.server.dataobject.ProductDO;
import com.student.server.dataobject.SnappedUserDO;
import com.student.server.kafkaTopics.Topics;
import com.student.server.model.OrderStatus;
import com.student.server.model.Product;
import com.student.server.model.Result;
import com.student.server.redisKeys.RedisConstant;
import com.student.server.service.SnappedService;
import com.student.server.util.UUIDUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class SnappedUpServiceImpl implements SnappedService {

    @Autowired
    private ProductDAO productDAO;

    @Autowired
    private SnappedUserDAO snappedUserDAO;

    @Autowired
    private OrderDAO orderDAO;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private RedissonClient redissonClient;



    // 脚本提前初始化，不要每次执行都new
    private static final DefaultRedisScript<Long> LUA_SCRIPT;

    static {
        LUA_SCRIPT = new DefaultRedisScript<>();
        LUA_SCRIPT.setScriptText(
                "local stockKey = KEYS[1]\n" +
                        "local userKey = KEYS[2]\n" +
                        "if redis.call('exists', userKey) == 1 then return -2 end\n" +
                        "local stock = tonumber(redis.call('get', stockKey) or 0)\n" +
                        "if stock <= 0 then return -1 end\n" +
                        "redis.call('decr', stockKey)\n" +
                        "redis.call('set', userKey, 1, 'EX', 21600)\n" +
                        "return 1"
        );
        LUA_SCRIPT.setResultType(Long.class);
    }

    /**
     * 抢购
     * @param productId
     * @param userId
     * @return
     */
    @Override
    public Result<Boolean> snappedUp(long productId, long userId) {
        Result<Boolean> result = new Result<>();

        if (productId <= 0 || userId <= 0) {
            result.setSuccess(false);
            result.setMessage("参数错误");
            result.setCode("500");
            return result;
        }

        String stockKey = RedisConstant.PRODUCT_STOCK_PREFIX + productId;
        String userKey = RedisConstant.USER_SNAPPED_PREFIX + userId + ":" + productId;

        // 缓存预热（高并发安全，只会执行一次）
        if (Boolean.FALSE.equals(redisTemplate.hasKey(stockKey))) {
            ProductDO productDO = productDAO.selectById(productId);
            if (productDO == null) {
                result.setSuccess(false);
                result.setMessage("商品不存在");
                result.setCode("404");
                return result;
            }
            // 原子写入
            redisTemplate.opsForValue().setIfAbsent(stockKey, productDO.getStock(), RedisConstant.STOCK_EXPIRE_SEC, TimeUnit.HOURS);
        }

        // 执行秒杀脚本
        Long execute = (Long) redisTemplate.execute(LUA_SCRIPT, Arrays.asList(stockKey, userKey));

        if (execute == -1) {
            result.setSuccess(false);
            result.setMessage("商品已抢光");
            result.setCode("500");
            return result;
        }
        if (execute == -2) {
            result.setSuccess(false);
            result.setMessage("您已抢购过该商品");
            result.setCode("500");
            return result;
        }
        if (execute != 1) {
            result.setSuccess(false);
            result.setMessage("抢购失败");
            result.setCode("500");
            return result;
        }

        ProductDO productDO = productDAO.selectById(productId);
        String productJsonStr = JSON.toJSONString(productDO);
        kafkaTemplate.send(Topics.KAFKA_SNAP_TOPIC, String.valueOf(userId), productJsonStr);

        //...kafka异步刷库,更新数据库库存和写入抢购成功用户

        result.setSuccess(true);
        result.setMessage("抢购成功");
        result.setCode("200");
        result.setData(true);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doSnapBusiness(long userId, long productId, Double price) {
        // 扣减数据库库存
        productDAO.reduceStock(productId, 1);

        //向数据库插入抢购成功用户
        SnappedUserDO snappedUserDO = new SnappedUserDO();
        snappedUserDO.setUserId(userId);
        snappedUserDO.setProductId(productId);
        snappedUserDAO.insert(snappedUserDO);

        //创建订单表并插入数据库
        OrderDO orderDO = new OrderDO();
        orderDO.setId(UUIDUtils.generateOrderId());
        orderDO.setProductId(String.valueOf(productId));
        orderDO.setUserId(userId);
        orderDO.setStatus(OrderStatus.PENDING_PAYMENT);
        orderDO.setTotalPrice(price);
        orderDO.setOrderNumber(generateOrderNumber());
        orderDAO.insertOrder(orderDO);

        String orderJsonStr = JSON.toJSONString(orderDO);
        kafkaTemplate.send(Topics.KAFKA_ORDER_TOPIC, String.valueOf(orderDO.getUserId()), orderJsonStr);
    }

    private String generateOrderNumber() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String now = LocalDate.now().format(dateTimeFormatter);
        String redisKey = "order:auto:increment:" + now;
        RAtomicLong atomicLong = redissonClient.getAtomicLong(redisKey);
        atomicLong.expire(6, TimeUnit.HOURS);
        long number = atomicLong.incrementAndGet();
        return now + "" + number;
    }


}

