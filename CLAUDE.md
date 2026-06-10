# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build the project
mvn clean package -DskipTests

# Run tests
mvn test

# Run a specific test class
mvn test -Dtest=UserControllerLoginTest

# Run the application
mvn spring-boot:run

# Run the packaged JAR
java -jar target/Server-0.0.1-SNAPSHOT.jar
```

## Tech Stack

- **Framework**: Spring Boot 3.5.13, Java 17 (compiler configured for 21)
- **Database**: MySQL via MyBatis (mapper XML in `src/main/resources/com/student/server/dao/`)
- **Cache**: Redis (session, stock, user data) + Caffeine (local user/product cache)
- **Message Queue**: Kafka (snapped-up, order emails, verification codes)
- **Auth**: JWT (io.jsonwebtoken 0.12.6), BCrypt password encoding
- **Real-time**: WebSocket (`/ws/user` endpoint for online user tracking)
- **Other**: Redisson (distributed lock, atomic long), Druid connection pool, Springdoc OpenAPI (Swagger), PageHelper pagination, Lombok

## Architecture

Standard layered Spring Boot architecture with **two controller layers**: `control/` serves web/Thymeleaf templates, `api/` serves mobile/REST clients (prefixed `/api/`). Both delegate to the same `service/` layer.

```
control/ + api/  →  service/ + service/impl/  →  dao/ (MyBatis mappers)  →  MySQL
                          ↓
                    cache/ (Caffeine)  +  Redis
                          ↓
                    consumer/ (Kafka listeners)
```

### Auth Flow
`UserInterceptor` intercepts all requests except login/register/forgot/Swagger/WebSocket/static paths. Validates `Authorization: Bearer <token>`, parses JWT into `UserInfo`, and sets it as `request.currentUser` attribute. Controllers read this attribute for the logged-in user.

### Rate Limiting (dual mechanism)
- **AOP-based** (`@RateLimit` annotation + `RateLimitAspect`): Declarative, per-method. Uses Redis Sorted Set + Lua for sliding window. Applied on login, register, forgot password, avatar upload, etc. Keys by userId when logged in, by IP otherwise.
- **Interceptor-based** (`RateLimiterInterceptor`): Applied only to `/api/product/snappedUp`. Prevents rapid-fire seckill requests.

### SecKill / SnappedUp Flow
The core business flow in `SnappedUpServiceImpl`:

1. **Redis Lua script** atomically checks stock (`product:stock:{id}`) and user duplicate (`user:snapped:{userId}:{productId}`), decrements stock if valid
2. On success, sends product info to Kafka topic `snappedUp`
3. `KafkaConsumer.listenSnap()` calls `doSnapBusiness()` which:
   - Reduces DB stock (SQL has `WHERE stock > 0` guard)
   - Inserts into `snapped_user` + `order` tables (unique key on userId+productId prevents duplicates)
   - Uses `TransactionSynchronizationManager` to rollback Redis on DB failure, and send order email via Kafka only after commit
4. `KafkaConsumer.listenOrder()` sends order confirmation email

### WebSocket Online Tracking
`UserWebSocketHandler` manages persistent connections at `/ws/user`. On connect, validates JWT from query param `?token=`, stores session in `ConcurrentHashMap<String, WebSocketSession>`, and adds user to Redis `online:users` sorted set (score = heartbeat timestamp). Client sends heartbeat every 30s. `OnlineUserTask` cleans up stale entries (>2min no heartbeat) every 30s. Online count is broadcast every 2s when it changes.

### Caching Strategy
- **Redis**: User objects (keyed by studentNum, 6h TTL), courses (Hash `course:all`, 3h TTL), seckill stock (6h), email verification codes (5min)
- **Caffeine** (`UserCache`): Individual users (1h expire, 10k max), total user count (10min expire). Used by `OnlineController` for batched online-user lookups.
- **Caffeine** (`SnappedUpServiceImpl.productCache`): Product info (10min expire, 500 max) to avoid DB hits during high-concurrency seckill.

### Kafka Topics (defined in `Topics.java`)
| Topic | Purpose | Consumer concurrency |
|---|---|---|
| `snappedUp` | SecKill DB writes | 3 |
| `order` | Order confirmation emails | 2 |
| `verifyCode` | Password-reset emails | 2 |
| `regCode` | Registration emails | 2 |

All consumers use manual acknowledgement (`ack.acknowledge()`).

### Key Redis Keys (defined in `RedisConstant.java`)
- `course:all` — Hash of all courses
- `product:stock:{id}` — Seckill product stock
- `user:snapped:{userId}:{productId}` — Duplicate seckill guard
- `emailCode:{studentNum}` / `register:code:{studentNum}` — Verification codes (5min TTL)
- `online:users` — Sorted set of online user IDs
- `rate:limiter:...` — Rate limit windows

### Key Config Properties
- JWT secret and expiration: `jwt.secret`, `jwt.expiration` (86400000ms = 24h)
- Email sender: `send.message.email`, `send.message.code` (QQ SMTP auth code)
- Kafka: `spring.kafka.bootstrap-servers`, manual ack mode
- Swagger UI: `/swagger-ui.html`, API docs: `/api-docs`
- Druid console: `/druid/*`
- File uploads served from: `{user.dir}/uploads/`
