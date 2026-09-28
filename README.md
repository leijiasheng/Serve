# 学生服务平台（Student Server）

> 一个基于 Spring Boot 3 的学生服务后端系统，覆盖**用户认证、选课、课程评论、在线状态追踪**四大业务域，并以 **Redis + Lua + Kafka** 实现了一套完整的**高并发秒杀链路**。

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.13-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MyBatis](https://img.shields.io/badge/MyBatis-3.0.5-red.svg)](https://mybatis.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [秒杀链路核心设计](#秒杀链路核心设计)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [API 一览](#api-一览)
- [项目结构](#项目结构)
- [已知问题与后续规划](#已知问题与后续规划)
- [License](#license)

---

## 功能特性

### 用户模块
- 注册 / 登录 / 登出，**JWT 无状态鉴权**（HS256，默认 24h 有效期）
- 密码 **BCrypt** 加密存储，响应体中密码字段通过 `@JsonSerialize(using = NullSerializer.class)` 强制置空
- 邮箱验证码找回密码（验证码经 Kafka 异步发送，Redis 存储 5 分钟）
- 头像上传、个人信息修改
- 管理端：用户列表分页查询、新增、删除

### 课程模块
- 课程列表查询（Redis Hash `course:all` 缓存，3h TTL）
- 按条件检索课程、新增课程
- 学生选课 / 退课 / 查询已选课程

### 秒杀模块（核心）
- Redis + Lua 脚本原子完成 **库存校验 + 重复抢购校验 + 扣减库存**
- Kafka 异步削峰，MySQL 落库与 Redis 扣减解耦
- **三层幂等保障**：Redis 用户标记 → Kafka 消费端 `DuplicateKeyException` 捕获 → 数据库联合唯一索引
- `TransactionSynchronizationManager` 实现事务补偿：回滚时归还 Redis 库存，提交后才发送订单邮件
- Redisson `RAtomicLong` 生成全局唯一订单号

### 评论 / 反馈模块
- 课程评论分页查询（PageHelper）、发布评论
- 用户反馈提交

### 在线状态模块
- WebSocket 长连接（`/ws/user`），JWT 通过 query param `?token=` 校验
- Redis ZSet (`online:users`) + 内存 `ConcurrentHashMap` 双结构管理会话
- 客户端 30s 心跳，定时任务清理 2 分钟无心跳的僵尸连接
- 在线人数变化时每 2s 广播一次

### 基础设施
- **双限流机制**：`@RateLimit` 注解 + AOP 切面（方法级），以及拦截器级限流（秒杀接口专用），底层均为 Redis ZSet 滑动窗口 + Lua
- 全局异常处理（`@RestControllerAdvice`）
- 两级缓存：Caffeine（本地，1h / 10min）+ Redis（分布式，6h / 3h）
- Druid 连接池 + 监控台、Swagger UI 在线接口文档
- Docker / docker-compose 一键部署

---

## 技术栈

| 分类 | 技术 | 版本 | 用途 |
|---|---|---|---|
| 语言 | Java | 17+ | — |
| 框架 | Spring Boot | 3.5.13 | 基础框架 |
| Web | Spring MVC + Thymeleaf | — | REST API + 服务端渲染页面 |
| 持久层 | MyBatis | 3.0.5 | SQL 映射，含自定义 `TypeHandler` |
| 分页 | PageHelper | 1.4.7 | 物理分页 |
| 数据库 | MySQL | 8.x | 主存储 |
| 连接池 | Druid | 1.2.21 | 含 SQL 监控 |
| 缓存 | Redis | — | 库存、验证码、会话、限流窗口、在线用户 |
| 本地缓存 | Caffeine | — | 用户 / 商品信息，抗热点 |
| 分布式锁 | Redisson | 3.26.0 | 原子长整型（订单号生成） |
| 消息队列 | Kafka | — | 秒杀削峰、异步邮件 |
| 认证 | jjwt | 0.12.6 | JWT 签发与校验 |
| 加密 | spring-security-crypto | — | BCrypt 编码器 |
| 实时通信 | Spring WebSocket | — | 在线用户追踪 |
| 文档 | Springdoc OpenAPI | 2.8.6 | Swagger UI |
| 工具 | Lombok / commons-io / commons-text / fastjson2 | — | — |

> **注意**：`pom.xml` 的 `properties` 中声明 `java.version` 为 17，但 `maven-compiler-plugin` 配置的 `source` / `target` 为 **21**（插件配置优先），实际编译目标为 Java 21。请使用 **JDK 21** 构建以避免不一致。

---

## 系统架构

### 分层结构

系统采用**双控制器层**设计：`control/` 面向 Web / Thymeleaf 模板，`api/` 面向移动端与 REST 客户端（统一 `/api/` 前缀），两者复用同一个 `service/` 层。

```
┌─────────────────────────────────────────────────────────────┐
│                          客户端                              │
│        Web 浏览器 (Thymeleaf)      移动端 / REST Client       │
└──────────┬───────────────────────────────┬──────────────────┘
           │                               │
           ▼                               ▼
┌──────────────────────┐       ┌──────────────────────┐
│  control/ (MVC)      │       │  api/ (REST)         │
│  UserController      │       │  UserAPI             │
│  CourseController    │       └──────────┬───────────┘
│  SnappedUpController │                  │
│  CommentController   │                  │
│  OnlineController    │                  │
└──────────┬───────────┘                  │
           └────────────┬─────────────────┘
                        ▼
        ┌───────────────────────────────────┐
        │          拦截器 / 切面链           │
        │   UserInterceptor      (JWT 鉴权)  │
        │   RateLimiterInterceptor (限流)    │
        │   RateLimitAspect   (@RateLimit)  │
        └───────────────┬───────────────────┘
                        ▼
        ┌───────────────────────────────────┐
        │         service/ + impl/          │
        │  UserService     CourseService    │
        │  SnappedService  CommentService   │
        │  UserCourseService  ProductService│
        └───┬──────────┬────────────┬───────┘
            │          │            │
            ▼          ▼            ▼
    ┌──────────┐ ┌─────────┐ ┌──────────────┐
    │  dao/    │ │ cache/  │ │    Redis     │
    │ MyBatis  │ │ Caffeine│ │  + Redisson  │
    └────┬─────┘ └─────────┘ └──────────────┘
         ▼                          ▲
    ┌─────────┐                     │
    │  MySQL  │                     │
    └─────────┘                     │
         ▲                          │
         │    ┌─────────────────────┴──────┐
         └────│  consumer/ (Kafka Listeners)│
              │  listenSnap / listenOrder  │
              │  listenVerifyCode          │
              │  listenRegCode             │
              └────────────────────────────┘
```

### 领域模型分层

| 层 | 包 | 说明 |
|---|---|---|
| DO（Data Object） | `dataobject/` | 与数据库表一一对应，如 `UserDO`、`ProductDO` |
| Model（业务/传输） | `model/` | 业务模型，如 `User`、`Order`、`LoginVO`、`Result<T>` |
| Param（入参） | `param/` | 请求参数对象，如 `PageParam` |
| 转换 | `xxx.toModel()` | DO → Model 转换，**密码等敏感字段不参与拷贝** |

### Kafka 主题

| Topic | 用途 | 消费者并发 |
|---|---|---|
| `snappedUp` | 秒杀落库（扣库存 + 写秒杀记录 + 写订单） | 3 |
| `order` | 订单确认邮件 | 2 |
| `verifyCode` | 找回密码验证码邮件 | 2 |
| `regCode` | 注册验证码邮件 | 2 |

- 生产者：`acks=all`、`enable.idempotence=true`、`retries=10`、`compression.type=snappy`、`linger.ms=5`
- 消费者：手动 ACK（`ack-mode: manual_immediate`）、`enable-auto-commit=false`、`auto-offset-reset=latest`

---

## 秒杀链路核心设计

这是本项目技术含量最高的部分。整体思路是 **「Redis 挡流量，Kafka 削峰，MySQL 保最终一致」**。

### 阶段一：缓存预热

应用启动时通过 `@PostConstruct` 将全部商品库存写入 Redis：

```java
@PostConstruct
public void initStock() {
    List<ProductDO> productDOList = productDAO.getAll();
    for (ProductDO productDO : productDOList) {
        String stockKey = RedisConstant.PRODUCT_STOCK_PREFIX + productDO.getId();
        redisTemplate.opsForValue().set(stockKey, productDO.getStock(),
                RedisConstant.STOCK_EXPIRE_SEC, TimeUnit.SECONDS);
    }
    log.info("缓存预热成功！");
}
```

### 阶段二：Redis + Lua 原子预扣减

秒杀请求先在 Redis 侧完成全部校验与扣减，**不碰数据库**：

```lua
-- KEYS[1] = product:stock:{productId}
-- KEYS[2] = user:snapped:{userId}:{productId}
-- 返回值：1 = 抢购成功；-1 = 库存不足；-2 = 重复抢购

if redis.call('exists', userKey) == 1 then return -2 end

local stock = tonumber(redis.call('get', stockKey) or 0)
if stock <= 0 then return -1 end

redis.call('decr', stockKey)
redis.call('set', userKey, 1, 'EX', 21600)
return 1
```

**为什么用 Lua**：Redis 单线程执行 Lua 脚本，脚本内的多条命令具备**原子性**，天然避免了「查库存 → 扣库存」之间的竞态；同时一次网络往返完成 4 步操作，比 `WATCH/MULTI` 乐观锁重试更稳定，也没有分布式锁的加解锁开销。

### 阶段三：Kafka 异步落库

Redis 预扣减成功后，将 `userId:productId` 发送到 `snappedUp` 主题，接口立即返回「排队中」，实现**削峰填谷**。

### 阶段四：消费端落库 + 事务补偿

`listenSnap()` 消费消息后进入 `doSnapBusiness()`：

```
开启事务
 ├─ 扣减 DB 库存（SQL 自带 WHERE stock > 0 兜底保护）
 ├─ 写入 snapped_user（联合唯一索引兜底）
 ├─ 写入 order（联合唯一索引兜底）
 └─ afterCommit → 发送 order 主题消息（触发邮件）
```

关键点在于 **`TransactionSynchronizationManager` 的两处同步回调**：

- **补偿回调**：注册在**所有数据库操作之前**（否则事务已进入提交阶段，回调来不及执行）。当 `afterCompletion` 收到 `STATUS_ROLLED_BACK` 时，执行 `INCR product:stock:{id}` 归还库存，并 `DEL` 用户抢购标记，让用户可以重试。
- **提交回调**：`afterCommit` 中才发送订单邮件消息 —— 避免「事务还没提交就把邮件发出去了，之后事务回滚」的不一致。

### 阶段五：三层幂等

| 层级 | 手段 | 拦截场景 |
|---|---|---|
| 1 | Redis `user:snapped:{userId}:{productId}` | 正常情况下的重复点击 |
| 2 | 消费端捕获 `DuplicateKeyException` → ack | Redis 标记因补偿被删除后的消息重投 |
| 3 | MySQL 联合唯一索引 `uk_user_product` | 兜底的最终防线 |

### 整体时序

```
用户 ──POST /api/product/snappedUp──► RateLimiterInterceptor（滑动窗口限流）
                                          │
                                          ▼
                                   Lua 脚本（原子预扣减）
                                   ┌──────┴──────┐
                                成功            失败
                                   │              └──► 返回「库存不足 / 重复抢购」
                                   ▼
                          发送 Kafka: snappedUp ──► 立即返回「排队中」
                                   │
                                   ▼
                         KafkaConsumer.listenSnap
                                   │
                            doSnapBusiness（事务）
                                   │
                         ┌─────────┴──────────┐
                     提交成功                回滚
                         │                    │
                   afterCommit         afterCompletion
                         │                    │
                  发 order 消息          INCR 库存 + DEL 标记
                         │
                   listenOrder → 发送邮件
```

---

## 快速开始

### 环境要求

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | **21**（或 17，见上方 pom 说明） | 编译与运行 |
| Maven | 3.8+ | 或使用项目自带 `mvnw` |
| MySQL | 8.0+ | 业务数据存储 |
| Redis | 6.0+ | 缓存 / 限流 / 在线状态 |
| Kafka | 3.x | 消息队列 |
| Docker | 可选 | 容器化部署 |

### 1. 克隆项目

```bash
git clone https://github.com/leijiasheng/Serve.git
cd Server
```

### 2. 初始化数据库

创建数据库（默认名 `serverdb`），然后执行唯一索引脚本：

```bash
mysql -u root -p serverdb < src/main/resources/db/init-index.sql
```

该脚本为 `snapped_user` 与 `order` 表补充联合唯一索引，是秒杀幂等的兜底防线，**必须执行**：

```sql
ALTER TABLE snappedUser ADD UNIQUE INDEX uk_user_product (user_id, product_id);
ALTER TABLE `order`    ADD UNIQUE INDEX uk_user_product (user_id, product_id);
```

> ⚠️ **注意**：该脚本末尾还会执行 `TRUNCATE TABLE user`（用于清空测试数据），**会删除全部用户记录**。在已有数据的环境执行前，请先手动去掉这两行：
>
> ```sql
> SET FOREIGN_KEY_CHECKS = 0;
> truncate table user;
> SET FOREIGN_KEY_CHECKS = 1;
> ```
>
> 另外，若表中已有重复的 `(user_id, product_id)` 记录，`ADD UNIQUE INDEX` 会失败，需先去重。

### 3. 配置中间件地址

`application.yml` 中**不含任何凭据**，所有敏感项均以 `${环境变量:默认值}` 形式声明。默认值指向本机（`127.0.0.1`），若中间件不在本机，通过环境变量覆盖即可。

本地开发推荐在项目根目录创建 `.env`（已被 `.gitignore` 忽略）：

```bash
cp .env.example .env
# 编辑 .env，填入数据库 / Redis / Kafka 地址与密码
```

或直接在 shell 中导出：

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/serverdb?serverTimezone=GMT%2B8'
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=<你的密码>
export SPRING_DATA_REDIS_HOST=127.0.0.1
export SPRING_DATA_REDIS_PORT=6379
export SPRING_KAFKA_BOOTSTRAP_SERVERS=127.0.0.1:9092

# 必填：至少 32 字节，留空会导致启动失败
export JWT_SECRET="$(openssl rand -base64 48)"
```

> `JWT_SECRET` 为空时，`JwtUtil` 构造 `SecretKey` 会抛出 `WeakKeyException`，应用无法启动。这是有意为之的**快速失败**设计——避免用一个可预测的默认密钥部署上线。

### 4. 构建与运行

```bash
# 构建（跳过测试）
mvn clean package -DskipTests

# 方式一：Maven 直接启动
mvn spring-boot:run

# 方式二：运行构建产物
java -jar target/Server-0.0.1-SNAPSHOT.jar
```

启动后访问：

| 地址 | 说明 |
|---|---|
| http://localhost:8081/ | 首页（Thymeleaf） |
| http://localhost:8081/swagger-ui.html | Swagger 接口文档 |
| http://localhost:8081/api-docs | OpenAPI JSON |
| http://localhost:8081/druid/ | Druid 监控台 |

### 5. Docker 部署

```bash
cp .env.example .env   # 填入数据库 / Redis / Kafka / JWT 等变量
docker compose up -d --build
```

Dockerfile 基于 `eclipse-temurin:17-jre`；`docker-compose.yml` 中不含任何明文凭据，全部通过环境变量注入（docker compose 会自动读取项目根目录的 `.env`），上传目录挂载为具名卷 `uploads-data`。

> 注意：`Dockerfile` 中 `EXPOSE 8085`，与 `application.yml` 默认的 `8081` 不同，容器部署时由 `docker-compose.yml` 的 `SERVER_PORT` 环境变量覆盖为 `8085`。

---

## 配置说明

所有配置项集中在 `src/main/resources/application.yml`，格式为 `${环境变量:默认值}`，文件中**不含任何真实凭据**。

| 配置项 | 环境变量 | 说明 |
|---|---|---|
| `server.port` | `SERVER_PORT` | 服务端口，默认 `8081` |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | MySQL JDBC URL，默认 `127.0.0.1:3306/serverdb` |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | 数据库用户名，默认 `root` |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | 数据库密码，默认空 |
| `spring.data.redis.host` | `SPRING_DATA_REDIS_HOST` | Redis 地址，默认 `127.0.0.1` |
| `spring.data.redis.port` | `SPRING_DATA_REDIS_PORT` | Redis 端口，默认 `6379` |
| `spring.data.redis.password` | `SPRING_DATA_REDIS_PASSWORD` | Redis 密码，默认空 |
| `spring.kafka.bootstrap-servers` | `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka 地址，默认 `127.0.0.1:9092` |
| `jwt.secret` | `JWT_SECRET` | JWT 签名密钥，**至少 32 字节，必填** |
| `jwt.expiration` | `JWT_EXPIRATION` | Token 有效期，默认 86400000ms（24h） |
| `send.message.email` | `SEND_MESSAGE_EMAIL` | 发件邮箱（QQ SMTP） |
| `send.message.code` | `SEND_MESSAGE_CODE` | QQ 邮箱 SMTP 授权码 |
| `upload.dir` | `UPLOAD_DIR` | 文件上传根目录，默认 `{user.dir}`，头像落在其下的 `uploads/` |
| `spring.datasource.druid.stat-view-servlet.enabled` | `DRUID_CONSOLE_ENABLED` | Druid 监控台开关，默认 `true` |
| `spring.datasource.druid.stat-view-servlet.login-username` | `DRUID_USERNAME` | Druid 控制台账号，默认 `admin` |
| `spring.datasource.druid.stat-view-servlet.login-password` | `DRUID_PASSWORD` | Druid 控制台密码，默认空 |

`docker-compose.yml` 同样只引用变量，实际值从项目根目录的 `.env` 读取（模板见 `.env.example`）。

### 关键 Redis Key 设计

| Key | 类型 | TTL | 用途 |
|---|---|---|---|
| `course:all` | Hash | 3h | 全部课程缓存 |
| `product:stock:{id}` | String | 6h | 秒杀商品库存 |
| `user:snapped:{userId}:{productId}` | String | 6h | 用户重复抢购标记 |
| `emailCode:{studentNum}` | String | 5min | 找回密码验证码 |
| `register:code:{studentNum}` | String | 5min | 注册验证码 |
| `online:users` | ZSet | — | 在线用户（score = 心跳时间戳） |
| `rate:limiter:snapped:user:{key}` | ZSet | 5s | 滑动窗口限流记录 |

限流参数：窗口 **1000ms**，窗口内最大请求数 **3** 次。

---

## API 一览

所有需要鉴权的接口通过请求头传递 Token：

```
Authorization: Bearer <jwt-token>
```

### 用户

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| POST | `/user/register` | 注册 | ✗ |
| POST | `/user/login` | 登录 | ✗ |
| POST | `/user/logout` | 登出 | ✓ |
| POST | `/user/forgot` | 找回密码 | ✗ |
| POST | `/user/update` | 修改个人信息 | ✓ |
| POST | `/user/handleUpdatePwd` | 修改密码 | ✓ |
| POST | `/user/uploadAvatar` | 上传头像 | ✓ |
| GET | `/user/userList` | 用户列表（分页） | ✓ |
| POST | `/user/insert` | 新增用户 | ✓ |
| POST | `/user/delete` | 删除用户 | ✓ |
| POST | `/user/sendCode` | 发送找回密码验证码 | ✗ |
| POST | `/user/sendRegCode` | 发送注册验证码 | ✗ |
| GET | `/person` | 个人中心页 | ✓ |
| POST | `/api/user/register` | 注册（REST） | ✗ |
| POST | `/api/user/login` | 登录（REST） | ✗ |
| GET | `/api/user/logout` | 登出（REST） | ✓ |
| POST | `/api/user/forgot` | 找回密码（REST） | ✗ |

### 课程 / 选课

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | `/api/course/get` | 获取课程列表 | ✓ |
| GET | `/api/course/getByCon` | 按条件查询课程 | ✓ |
| POST | `/api/course/add` | 新增课程 | ✓ |
| POST | `/api/userCourse/insert` | 选课 | ✓ |
| GET | `/api/userCourse/get` | 查询已选课程 | ✓ |
| POST | `/api/userCourse/delete` | 退课 | ✓ |

### 秒杀

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | `/api/product/list` | 秒杀商品列表 | ✓ |
| GET | `/api/product/snappedUp` | 秒杀下单 | ✓ |

### 评论 / 反馈 / 在线状态

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | `/comment/list` | 评论列表（分页） | ✓ |
| POST | `/comment/publish` | 发布评论 | ✓ |
| POST | `/api/feedback` | 提交反馈 | ✓ |
| GET | `/online/count` | 当前在线人数 | ✓ |
| GET | `/online/stats` | 在线统计 | ✓ |
| GET | `/online/list` | 在线用户列表 | ✓ |
| WS | `/ws/user?token=<jwt>` | 在线状态 WebSocket | ✓ |

> `/online/**` 未加入拦截器排除列表，因此**需要携带 Token**；而 `/ws/**` 已排除，JWT 改由 query param `?token=` 传递。

### 数据初始化

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | `/init` | 批量初始化数据 | ✗ |
| GET | `/findByStudentNum` | 按学号查询 | ✓ |
| GET | `/find` | 查询 | ✓ |
| GET | `/comments` | 初始化评论 | ✓ |
| GET | `/getCookies` | 读取 Cookie | ✓ |

> 仅 `/init` 精确路径被排除在鉴权之外，同控制器下的其余接口仍需 Token。

### 限流阈值（`@RateLimit`，AOP 切面）

| 方法 | 接口 | 窗口 | 上限 |
|---|---|---|---|
| POST | `/user/register` | 60s | 5 |
| POST | `/user/login` | 60s | 5 |
| POST | `/user/forgot` | 60s | 2 |
| POST | `/user/update` | 60s | 3 |
| POST | `/user/handleUpdatePwd` | 60s | 2 |
| POST | `/user/uploadAvatar` | 60s | 5 |
| POST | `/user/sendCode` | 60s | 1 |
| POST | `/api/user/register` | 60s | 5 |
| POST | `/api/user/login` | 60s | 5 |
| POST | `/api/user/forgot` | 60s | 3 |
| GET | `/api/product/snappedUp` | 1000ms | 3（拦截器级） |

---

## 项目结构

```
Server/
├── src/main/java/com/student/server/
│   ├── ServerApplication.java           # 启动类
│   ├── annotation/RateLimit.java        # @RateLimit 限流注解
│   ├── api/UserAPI.java                 # REST 控制器层
│   ├── aspect/RateLimitAspect.java      # 限流切面（滑动窗口 Lua）
│   ├── cache/UserCache.java             # Caffeine 本地缓存
│   ├── config/                          # 配置类
│   │   ├── CorsConfig.java
│   │   ├── RedisTemplateInit.java       # 自定义 RedisTemplate 序列化
│   │   ├── SwaggerConfig.java
│   │   ├── WebAppConfiguration.java     # 拦截器注册与排除路径
│   │   └── SpringHttpSessionConfig.java
│   ├── consumer/KafkaConsumer.java      # 4 个 Topic 的消费者
│   ├── control/                         # Web / Thymeleaf 控制器层
│   ├── dao/                             # MyBatis Mapper 接口
│   ├── dataobject/                      # DO，对应数据库表
│   ├── email/EmailClient.java           # 邮件发送
│   ├── exception/                       # 全局异常处理
│   ├── interceptor/                     # UserInterceptor / RateLimiterInterceptor
│   ├── kafkaTopics/Topics.java          # Topic 常量
│   ├── model/                           # 业务模型 / VO / Result
│   ├── param/                           # 请求参数
│   ├── redisKeys/RedisConstant.java     # Redis Key 与限流参数常量
│   ├── service/ + service/impl/         # 业务逻辑层
│   ├── task/OnlineUserTask.java         # 僵尸连接清理定时任务
│   ├── toDo/ToDo.java                   # 开发待办与已知缺陷记录
│   ├── util/                            # JWT、TypeHandler、UUID 工具
│   └── websocket/                       # WebSocket 配置与 Handler
│
├── src/main/resources/
│   ├── application.yml                  # 主配置（全部为 ${环境变量:默认值}，无凭据）
│   ├── com/student/server/dao/*.xml     # MyBatis Mapper XML
│   ├── db/init-index.sql                # 唯一索引初始化脚本
│   ├── data/userData.json               # 初始化数据
│   ├── static/                          # 静态资源
│   └── templates/                       # Thymeleaf 页面
│
├── src/test/java/com/student/server/    # 单元测试
├── .env.example                         # 环境变量模板（复制为 .env 后填写）
├── Dockerfile
├── docker-compose.yml                   # 仅引用环境变量，无明文凭据
├── pom.xml
└── README.md
```

---

## 已知问题与后续规划

项目在开发过程中维护了一份自查清单（`src/main/java/com/student/server/toDo/ToDo.java`），以下是当前已知的**主要问题**与改进方向，欢迎以 Issue / PR 形式讨论。

### 正确性与安全

- [ ] **`UserServiceImpl.resetPwd()` 将密码重置为固定值 `123456`** —— 存在账号接管风险，应改为随机密码 + 首次登录强制修改，或走邮件重置链接
- [ ] **验证码校验通过后未删除** —— 存在 5 分钟窗口内重放的风险；且验证码使用 `java.util.Random` 生成，应改用 `SecureRandom`
- [ ] **`/init` 等初始化接口被排除在鉴权之外** —— 生产环境必须关闭或加管理员校验
- [ ] **文件上传仅校验客户端传入的 `Content-Type`，扩展名取自 `getOriginalFilename()`** —— 应改为服务端探测真实文件类型 + 白名单扩展名 + 重命名存储
- [ ] **限流依赖 `X-Forwarded-For` 头** —— 该头可被伪造，需要由可信代理覆写或改用真实连接 IP
- [ ] **Redis 反序列化开启 `activateDefaultTyping(LaissezFaireSubTypeValidator, ...)`** —— 若 Redis 实例对外暴露，存在 Jackson 反序列化 RCE 风险
- [ ] **CORS 配置 `allowedOriginPattern("*")` 搭配 `allowCredentials(true)`** —— 应改为明确的白名单来源
- [ ] **Druid 控制台默认启用，账号默认 `admin`、密码默认空** —— 生产环境应设置 `DRUID_CONSOLE_ENABLED=false`，或改为强口令并限制来源 IP

### 一致性与并发

- [ ] **Kafka `send()` 失败未被处理** —— Redis 库存已扣减但消息丢失会造成少卖；应引入本地消息表或事务消息做补偿
- [ ] **重复消费抛出的 `DuplicateKeyException` 会回滚事务并触发补偿逻辑，导致库存被「归还」** —— 可能造成超卖，需在补偿前区分回滚原因
- [ ] **`afterCommit` 发送失败后没有重试机制**
- [ ] **Redis 库存 key 6 小时后过期，Lua 中 `get(KEY) or 0` 会把「key 不存在」当作「库存为 0」** —— 应显式区分并回源数据库
- [ ] **Redis 库存为纯内存态，重启即丢失** —— 需要持久化，或启动时以数据库为准重建

### 工程化

- [ ] **选课 `insertUserCourse` 缺少 `@Transactional`，且 `user_course` 表无唯一索引** —— 存在先查后改的竞态，且两步写入可能不一致
- [ ] **新增课程后未失效 `course:all` 缓存** —— 最长 3 小时内新课程对用户不可见
- [ ] **`OnlineUserTask` 只清理 Redis，不清理由 JVM 持有的 `sessions` / `sessionToUser`** —— 长期运行会造成内存泄漏
- [ ] **在线人数计数器在重连与 `userId == null` 场景下会漂移甚至为负**
- [ ] **限流 Lua 脚本在 `RateLimitAspect` 与 `RateLimiterInterceptor` 中完全重复** —— 应抽取为共享常量或独立组件
- [ ] **`UserServiceImplLoginTest` 仍使用 MD5 构造密码，而实现已迁移至 BCrypt** —— 该测试当前应处于失败状态，需同步修复
- [ ] **拦截器内 JWT 被解析两次**（`validateToken` 与 `getUserInfoFromToken`）
- [ ] **`EmailClient.sendOrderEmail` 使用 `System.getProperties()`，污染全局 JVM 属性** —— 应改为 `new Properties()`
- [ ] **`CommentController` 对评论数据查询两次**（一次取数据、一次由 PageHelper 统计总数）
- [ ] **`SpringHttpSessionConfig` 为空类**，且 `spring-session-data-redis` 依赖未被实际使用
- [ ] **`UserDAO.xml` 中 `<if test="personSign != ''">` 会导致该字段无法被清空**
- [ ] **Druid 使用默认连接池参数**（`maxActive=8`），与 Tomcat 线程数不匹配；Redis / Kafka 均未设置超时

### 规划方向

- 引入 **RocketMQ 事务消息** 或 **本地消息表** 解决 Redis 与 DB 的最终一致性
- 秒杀库存改为 **Redis 持久化 + 定时对账** 双保险
- 补充限流、秒杀链路的**集成测试**（Testcontainers）
- 前端页面与后端接口分离，`control/` 层逐步收敛为纯 REST

---

## License

本项目采用 [MIT License](LICENSE) 授权。