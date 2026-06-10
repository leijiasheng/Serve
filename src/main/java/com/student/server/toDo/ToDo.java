package com.student.server.toDo;

public class ToDo {

    //解决注册和忘记密码公用success和error页面问题

    //解决result存储信息得关键字导致得返回按钮得问题

    //解决忘记密码后数据库密码更改，但是redis依然存有旧的密码导致登录不进去问题

    //修改密码失效问题

    //完成注册，忘记密码，修改密码等返回的成功及失败页面得规范性

    //以上已解决

    //20260404

    //解决数据直接插入但是登录不进去的问题，

    //解决删除数据库用户和缓存用户以及缓存里面的登录信息残留问题,也就是session写入redis的时候key设置为studentNum；

    //20260407

    //解决输入内容转义问题

    //解决分页查询和实现赖加载

    //20260418

    //解决发布评论后昵称失效问题

    //实现session失效后跳转到login

    //请求头参数封装为application/json

    //20260424
    //选课中心查不到数据

    //20260427
    //将选课信息写入redis
    //评论系统子评论无法挂载到父评论
    //有空的时候，把所有的localstorage全部都改成sessionStorage

    //20260430
    //删除status

    //20260503
    //解决压测失效问题
    //lua脚本学习

    //20260506
    //解决redis里面意外出现的分布式ID键值对

    //20260507
    //解决由于事务回滚导致的redis数据扣减与数据库不一致
    //前后端改用axios调用不用fetch调用


    //抢购失败问题
    //抢购之后还能继续抢购其他的

    // 拦截器 vs AOP 的差异
    //
    //  HandlerInterceptor（旧）           AOP（新）
    //  ─────────────────                 ─────────────
    //  Servlet 层拦截                    方法调用层拦截
    //  手动写 response                   抛异常 → @RestControllerAdvice 统一处理
    //  需要在 WebAppConfiguration 注册   只需 @Aspect + @Component，自动生效
    //  无法拦截 Service 层方法            可拦截任意 Bean 方法

    //解决delete用户强制踢出问题


    /**
     * 20260531
     */


    //1（严重）. GET 请求做写操作
    //
    //  // SnappedUpController.java:53
    //  @GetMapping("/snappedUp")    // ← GET?!
    //  public Result<Boolean> snappedUp(...)
    //
    //  问题：改变服务器状态（扣库存、写 Kafka）却用 GET，违反 REST
    //  规范。浏览器会预加载、搜索引擎会爬取、中间代理可能缓存，都可能误触发。
    //
    //  修复：
    //  @PostMapping("/snappedUp")
    //
    //  ---
    //  2（严重）. Kafka 发送失败没回滚 Redis
    //
    //  // SnappedUpServiceImpl.java:134-136
    //  ProductDO productDO = productCache.get(productId, ...);
    //  String productJsonStr = JSON.toJSONString(productDO);
    //  kafkaTemplate.send(Topics.KAFKA_SNAP_TOPIC, ...);  // ← 如果这里失败？
    //  // Redis 已经扣过了，Kafka 没发出去 → 用户丢了资格但没下单
    //
    //  修复：
    //  try {
    //      kafkaTemplate.send(...).get(3, TimeUnit.SECONDS);
    //  } catch (Exception e) {
    //      // 回滚 Redis
    //      redisTemplate.opsForValue().increment(stockKey, 1);
    //      redisTemplate.delete(userKey);
    //      result.setMessage("系统繁忙，请重试");
    //      return result;
    //  }
    //
    //  ---
    //  3（中等）. Redis 库存无持久化保障
    //
    //  // 启动时加载一次
    //  @PostConstruct
    //  public void initStock() { ... }
    //  // TTL 6小时后过期
    //  redisTemplate.opsForValue().set(stockKey, ..., 21600, TimeUnit.SECONDS);
    //
    //  问题：如果 Redis 重启、key 过期、或新增商品，Redis 库存就没了。而且 get stockKey or 0 会把过期的当 0
    //  直接返回"已抢光"。
    //
    //  修复：加定时刷新或懒加载兜底：
    //
    //  // 在 Lua 脚本里加回源逻辑（伪代码）
    //  if redis.call('exists', stockKey) == 0 then
    //      local dbStock = loadFromDB(productId)   // 或启动时预热的兜底缓存
    //      redis.call('set', stockKey, dbStock)
    //  end
    //
    //  ---
    //  4（中等）. 用户反馈模糊
    //
    //  result.setMessage("若你抢购成功后会以邮件形式通知你。");
    //
    //  问题：Redis 扣成功了但 Kafka 消费 / DB 写失败了，用户看到"成功会通知你"，然后就没然后了，既没邮件也没提示。
    //
    //  建议：加一个查询接口让用户自己查结果，或者即使失败了也发一封"未抢到"的邮件。
    //
    //  ---
    //  5（中等）. Caffeine 缓存的 ProductDO 直接序列化到 Kafka
    //
    //  // 第134行
    //  ProductDO productDO = productCache.get(productId, key -> productDAO.selectById(key));
    //  String productJsonStr = JSON.toJSONString(productDO);
    //  kafkaTemplate.send(..., productJsonStr);  // ← 10分钟内都是同一份缓存数据
    //
    //  问题：如果活动期间改价格，缓存里的旧价格可能要 10 分钟才刷新，导致订单价格错误。
    //
    //  修复：只序列化必要字段（id + price），并且价格从 Redis 实时取而不是从缓存取。
    //
    //  ---
    //  6（轻微）. afterCommit 发 Kafka 无重试
    //
    //  // 第214行
    //  kafkaTemplate.send(Topics.KAFKA_ORDER_TOPIC, ...);  // 失败呢？
    //
    //  问题：事务已提交，订单已入库，但如果没有捕获异常，邮件永不发送。没有死信队列或补偿任务来重试。
    //
    //  ---
    //  7（轻微）. MyBatis SQL 的 <if> 隐患
    //
    //  <update id="reduceStock">
    //      update product
    //      set gmt_modified = now(),
    //      <if test="count > 0">
    //          stock = stock - #{count}
    //      </if>
    //      where id = #{id} and stock > 0
    //  </update>
    //
    //  如果 count <= 0（虽然传参始终为 1），SQL 变成 update product set gmt_modified=now() where id=? and stock >
    //  0——只更新了时间，没扣库存但更新行数 > 0，逻辑判定成功。虽然不太会发生，但不如直接 stock = stock - #{count} 去掉 <if>。


    // 修复范围就是刚才列出来的那几个问题：
    //  1. GET 改 POST — 简单
    //  2. Kafka 发送失败回滚 Redis — 关键
    //  3. Redis 库存懒加载兜底 — 防过期
    //  4. Caffeine 缓存数据不直接序列化到 Kafka — 价格准确性
    //  5. afterCommit Kafka 发送加错误处理 — 邮件可靠性
    //  6. SQL <if> 移除 — 代码健壮性
}
