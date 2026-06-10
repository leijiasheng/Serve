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
import com.student.server.model.Result;
import com.student.server.redisKeys.RedisConstant;
import com.student.server.service.SnappedService;
import com.student.server.util.UUIDUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
@Slf4j
public class SnappedUpServiceImpl implements SnappedService {

    private final ProductDAO productDAO;

    private final SnappedUserDAO snappedUserDAO;

    private final OrderDAO orderDAO;

    private final RedisTemplate<String, Object> redisTemplate;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final RedissonClient redissonClient;


    // 商品信息本地缓存
    private final Cache<Long, ProductDO> productCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

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

    @PostConstruct
    public void initStock() {
        List<ProductDO> productDOList = productDAO.getAll();
        for (ProductDO productDO : productDOList) {
            String stockKey = RedisConstant.PRODUCT_STOCK_PREFIX + productDO.getId();
            redisTemplate.opsForValue().set(stockKey, productDO.getStock(), RedisConstant.STOCK_EXPIRE_SEC, TimeUnit.SECONDS);
        }
        log.info("缓存预热成功！");
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

        ProductDO productDO = productCache.get(productId, key -> productDAO.selectById(key));
        String productJsonStr = JSON.toJSONString(productDO);
        kafkaTemplate.send(Topics.KAFKA_SNAP_TOPIC, String.valueOf(userId), productJsonStr);

        //...kafka异步刷库,更新数据库库存和写入抢购成功用户

        result.setSuccess(true);
        result.setMessage("若你抢购成功后会以邮件形式通知你。");
        result.setCode("200");
        result.setData(true);
        return result;
    }

    /**
     * 通过kafka监听，异步更新数据库库存和写入抢购成功用户
     * @param userId
     * @param productId
     * @param price
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doSnapBusiness(long userId, long productId, Double price) {

        //事务结束或回滚后自动触发，前提是在回滚或结束之前已经注册
        //注意要在所有数据库事务之前，否则来不及注册导致未执行
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            //事务完成（不管成功 OR 回滚都会进）
            @Override
            public void afterCompletion(int status) {
                // 事务回滚了
                if (status == STATUS_ROLLED_BACK) {
                    String stockKey = RedisConstant.PRODUCT_STOCK_PREFIX + productId;
                    String userKey = RedisConstant.USER_SNAPPED_PREFIX + userId + ":" + productId;

                    // 回滚 Redis
                    redisTemplate.opsForValue().increment(stockKey, 1);
                    redisTemplate.delete(userKey);

                    log.warn("事务回滚，已自动修复Redis库存：userId={}, productId={}", userId, productId);
                }
            }
        });

        // 扣减数据库库存（SQL 已带 stock > 0 条件，防止扣负）
        int stockRows = productDAO.reduceStock(productId, 1);
        if (stockRows <= 0) {
            throw new RuntimeException("库存扣减失败，商品库存不足或不存在");
        }

        /**
         * order和snappedUser表中 user_id 和 product_id 共同组成唯一键 unique key，保证事务回滚正常
         */

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

        // ==============================================
        // 事务真正成功后，再发送订单消息（更严谨）
        // ==============================================
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            //事务提交成功才会进入
            @Override
            public void afterCommit() {
                // 事务提交成功才发邮件
                String orderJsonStr = JSON.toJSONString(orderDO);
                kafkaTemplate.send(Topics.KAFKA_ORDER_TOPIC, String.valueOf(orderDO.getUserId()), orderJsonStr);
            }
        });
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

