package com.student.server.consumer;

import com.alibaba.fastjson2.JSON;
import com.student.server.dao.ProductDAO;
import com.student.server.dao.UserDAO;
import com.student.server.dataobject.OrderDO;
import com.student.server.dataobject.ProductDO;
import com.student.server.dataobject.UserDO;
import com.student.server.email.EmailClient;
import com.student.server.kafkaTopics.Topics;
import com.student.server.service.SnappedService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class KafkaConsumer {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private ProductDAO productDAO;

    @Autowired
    private EmailClient emailClient;

    @Autowired
    private SnappedService snappedService;

    /**
     * 监听抢购成功消息，用于写入刷新数据库
     * @param record
     */
    @KafkaListener(topics = Topics.KAFKA_SNAP_TOPIC)
    public void listenSnap(ConsumerRecord<String, String> record) {
        try {
            // 直接获取消息
            String userIdStr = record.key();
            String productJsonStr = record.value();
            ProductDO productDO = JSON.parseObject(productJsonStr, ProductDO.class);

            long userId = Long.parseLong(userIdStr);
            long productId = productDO.getId();
            Double price = productDO.getPrice();

            snappedService.doSnapBusiness(userId, productId, price);

            log.info("kafka消费成功，用户ID：{}, 商品：{}", userId, productDO);
        } catch (Exception e) {
            log.error("kafka消费失败，异常信息：", e);
        }
    }

    /**
     * 监听订单消息，用于异步发送订单到邮箱
     * @param record
     */
    @KafkaListener(topics = Topics.KAFKA_ORDER_TOPIC)
    public void listenOrder(ConsumerRecord<?, ?> record) {
       try {
           String userIdStr = String.valueOf(record.key());
           String orderJsonStr = String.valueOf(record.value());

           UserDO userDO = userDAO.selectByUserId(Long.parseLong(userIdStr));
           OrderDO orderDO = JSON.parseObject(orderJsonStr, OrderDO.class);

           String productId = orderDO.getProductId();
           ProductDO productDO = productDAO.selectById(Long.parseLong(productId));

           orderDO.setProduct(productDO.toModel());
           orderDO.setUser(userDO.toModel());

           //根据userId查询user在查询email
           String userEmail = userDO.getEmail();

           emailClient.sendOrderEmail(userEmail, JSON.toJSONString(orderDO));

           log.info("订单邮件已发送至：{}，用户昵称：{}", userEmail, userDO.getNickName());
       } catch (Exception e) {
           log.error("订单邮件发送失败", e);
       }
    }

    @KafkaListener(topics = Topics.KAFKA_CODE_TOPIC)
    public void listenVerifyCode(ConsumerRecord<?, ?> record) {
        try {
            String emailStr = String.valueOf(record.key());
            String codeNumStr = String.valueOf(record.value());

            emailClient.sendVerifyCodeEmail(emailStr, codeNumStr);
            log.info("验证码邮件已发送至：{}", emailStr);
        } catch (Exception e) {
            log.error("验证码邮件发送失败", e);
        }
    }



}
