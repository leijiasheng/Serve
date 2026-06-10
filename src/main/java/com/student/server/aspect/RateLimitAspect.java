package com.student.server.aspect;

import com.student.server.annotation.RateLimit;
import com.student.server.exception.RateLimitException;
import com.student.server.model.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.UUID;

@Slf4j
@Aspect
@Component
//基于 Redis + Lua + Spring AOP 实现的【滑动窗口接口限流切面】，作用是：控制接口访问频率，防止恶意刷接口、压垮服务。
// AOP 切面，@Around("@annotation(rateLimit)") 环绕通知执行 Redis 滑动窗口限流，被限时抛 RateLimitException
public class RateLimitAspect {

    private final StringRedisTemplate stringRedisTemplate;

    private final DefaultRedisScript<Long> slidingWindowScript;

    public RateLimitAspect(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        String lua = "local key = KEYS[1]\n" +
                //当前时间戳（毫秒/秒）转数字
                "local now = tonumber(ARGV[1])\n" +
                // 滑动窗口开始时间（比如现在往前推1秒）
                "local windowStart = tonumber(ARGV[2])\n" +
                //窗口最大允许请求数（比如10次
                "local maxRequests = tonumber(ARGV[3])\n" +
                //键过期时间（自动清理）
                "local ttlSeconds = tonumber(ARGV[4])\n" +
                //本次请求唯一标识（如请求ID/时间戳）
                "local member = ARGV[5]\n" +
                //【核心】删除窗口之外的旧数据（滑动窗口的灵魂）
                "redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)\n" +
                //统计当前窗口内有多少请求
                "local count = redis.call('ZCARD', key)\n" +
                "if count >= maxRequests then\n" +
                "    return 0\n" +
                "end\n" +
                // 没超，把本次请求加入窗口
                "redis.call('ZADD', key, now, member)\n" +
                // 设置过期时间，防止无限占用内存
                "redis.call('EXPIRE', key, ttlSeconds)\n" +
                "return 1";
        this.slidingWindowScript = new DefaultRedisScript<>(lua, Long.class);
    }

    //定义一个环绕通知，拦截所有标有 @RateLimit 注解的方法，并在方法执行前后进行增强处理。
    @Around("@annotation(rateLimit)")
    public Object doRateLimit(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        UserInfo userInfo = (UserInfo) request.getAttribute("currentUser");

        String className = pjp.getTarget().getClass().getSimpleName();
        String methodName = pjp.getSignature().getName();

        // 未登录时按 IP 限流（登录/注册/验证码等接口），已登录按 userId 限流
        String key;
        if (userInfo != null) {
            key = "rate:limiter:anno:" + className + ":" + methodName + ":" + userInfo.getId();
        } else {
            key = "rate:limiter:anno:" + className + ":" + methodName + ":ip:" + getClientIp(request);
        }

        long now = System.currentTimeMillis();
        long windowStart = now - rateLimit.windowMs();
        String member = now + ":" + UUID.randomUUID().toString().replaceAll("-", "");
        long ttlSeconds = rateLimit.windowMs() / 1000 + 1;

        Long result = stringRedisTemplate.execute(
                slidingWindowScript,
                //快速创建【只有一个元素的不可变 List】** 的方法。
                //Redis 执行 Lua 脚本 时，要求 KEYS（Redis 键）必须以 List 形式传入。
                Collections.singletonList(key),
                String.valueOf(now),
                String.valueOf(windowStart),
                String.valueOf(rateLimit.maxRequests()),
                String.valueOf(ttlSeconds),
                member
        );

            if (result == 0L) {
                String identifier = userInfo != null ? String.valueOf(userInfo.getId()) : getClientIp(request);
                log.warn("Rate limit exceeded for {}#{} {}", className, methodName, identifier);
                throw new RateLimitException(rateLimit.message());
            }

            return pjp.proceed();
        }

        //根据 Redis 限流结果判断是否拦截请求，并获取用户真实 IP 做限流标识，是标准的滑动窗口限流切面逻辑。
        private String getClientIp(HttpServletRequest request) {
            String[] headers = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
            String ip = null;
            for (String header : headers) {
                ip = request.getHeader(header);
                if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) break;
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        }
}
