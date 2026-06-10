-- 防止重复秒杀
ALTER TABLE snappedUser ADD UNIQUE INDEX uk_user_product (user_id, product_id);

-- 防止重复订单
ALTER TABLE `order` ADD UNIQUE INDEX uk_user_product (user_id, product_id);

SET FOREIGN_KEY_CHECKS = 0;
truncate table user;
SET FOREIGN_KEY_CHECKS = 1;
