package com.student.server.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.server.model.Result;
import com.student.server.model.UserInfo;
import com.student.server.redisKeys.RedisConstant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.UUID;

/**
 * 这就是一个 完全自定义、基于 Redis + Lua + Spring 拦截器 实现的【全局 / 接口限流拦截器】
 */
@Slf4j
@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final StringRedisTemplate stringRedisTemplate;

    private final DefaultRedisScript<Long> slidingWindowScript;

    public RateLimiterInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        String lua = "local key = KEYS[1]\n" +
                "local now = tonumber(ARGV[1])\n" +
                "local windowStart = tonumber(ARGV[2])\n" +
                "local maxRequests = tonumber(ARGV[3])\n" +
                "local ttlSeconds = tonumber(ARGV[4])\n" +
                "local member = ARGV[5]\n" +
                "redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)\n" +
                "local count = redis.call('ZCARD', key)\n" +
                "if count >= maxRequests then\n" +
                "    return 0\n" +
                "end\n" +
                "redis.call('ZADD', key, now, member)\n" +
                "redis.call('EXPIRE', key, ttlSeconds)\n" +
                "return 1";

        this.slidingWindowScript = new DefaultRedisScript<>(lua, Long.class);
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        UserInfo userInfo = (UserInfo) request.getAttribute("currentUser");
        if (userInfo == null) {
            return true;
        }

        long now = System.currentTimeMillis();
        long windowStart = now - RedisConstant.RATE_LIMIT_WINDOW_MS;
        String member = now + ":" + UUID.randomUUID().toString().replaceAll("-", "");

        Long result = stringRedisTemplate.execute(
                slidingWindowScript,
                Collections.singletonList(RedisConstant.RATE_LIMIT_KEY_PREFIX + userInfo.getId()),
                String.valueOf(now),
                String.valueOf(windowStart),
                String.valueOf(RedisConstant.RATE_LIMIT_MAX_REQUESTS),
                String.valueOf(RedisConstant.RATE_LIMIT_TTL_SEC),
                member
        );

        if (result == 0L) {
            log.warn("Rate limit exceeded for user {} on snappedUp", userInfo.getId());
            response.setContentType("application/json;charset=utf-8");
            response.setStatus(429);
            Result<Object> resp = new Result<>();
            resp.setSuccess(false);
            resp.setMessage("请勿频繁操作");
            OBJECT_MAPPER.writeValue(response.getWriter(), resp);
            return false;
        }
        return true;
    }
}
