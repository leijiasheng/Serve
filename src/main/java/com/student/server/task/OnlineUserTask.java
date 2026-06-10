package com.student.server.task;

import com.student.server.websocket.UserWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OnlineUserTask {

    private static final String ONLINE_USERS_KEY = "online:users";

    private final StringRedisTemplate stringRedisTemplate;
    private final UserWebSocketHandler userWebSocketHandler;

    /**
     * 每 30s 清理一次超过 2 分钟未心跳的在线记录(网络异常导致)，
     * 清理后同步内存计数器，防止 onlineCount 漂移。
     */
    @Scheduled(fixedRate = 30000)
    public void cleanStaleOnlineUsers() {
        long threshold = System.currentTimeMillis() - 120_000;
        Long removed = stringRedisTemplate.opsForZSet().removeRangeByScore(ONLINE_USERS_KEY, 0, threshold);
        if (removed != null && removed > 0) {
            log.info("在线用户清理：移除 {} 个过期连接", removed);
        }
        userWebSocketHandler.setOnlineCountFromRedis();
    }
}
