package com.student.server.websocket;

import com.alibaba.fastjson2.JSON;
import com.student.server.model.UserInfo;
import com.student.server.util.JwtUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * WebSocket 消息处理器，专门负责管理用户的实时连接：
 * 用户登录后建立长连接、下线断开连接、维持在线状态、记录谁在线。
 */
@Slf4j
@Component
public class UserWebSocketHandler extends TextWebSocketHandler {

    //Redis 里存在线用户列表的 key
    private static final String ONLINE_USERS_KEY = "online:users";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * key：用户 ID
     * value：该用户的 WebSocket 连接（session）
     * 作用：给用户发消息用
     */
    // userId -> WebSocketSession
    private final ConcurrentHashMap<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * key：Spring 自动生成的 sessionId
     * value：用户 ID
     * 作用：连接断开时，快速找到是哪个用户下线了
     */
    // sessionId -> userId（用于可靠查找断开的连接），用于踢出离线用户
    private final ConcurrentHashMap<String, Long> sessionToUser = new ConcurrentHashMap<>();

    // 内存计数器，代替每次广播都查 Redis ZCard
    private final AtomicInteger onlineCount = new AtomicInteger(0);
    // 上次广播的值，用于定时任务判断是否变化
    private volatile int lastBroadcastCount = -1;
    // scheduledBroadcast 轮次计数器，用于定期校准
    private int broadcastRound = 0;

    //注入构造器，引入实例
    public UserWebSocketHandler(JwtUtil jwtUtil, StringRedisTemplate stringRedisTemplate) {
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 服务重启后从 Redis 恢复 onlineCount，避免从 0 开始与 Redis 不一致
    @PostConstruct
    public void initOnlineCount() {
        Long count = stringRedisTemplate.opsForZSet().zCard(ONLINE_USERS_KEY);
        int c = count != null ? count.intValue() : 0;
        onlineCount.set(c);
        lastBroadcastCount = c;
        if (c > 0) {
            log.info("从 Redis 恢复在线人数: {}", c);
        }
    }

    //客户端建立连接时执行 session：当前连接对象
    // 1. 从 ws 连接地址里拿到 token
    // 2. 校验 token 是否合法（是否登录）
    // 3. 拿到 userId
    // 4. 如果这个用户已经在线，挤掉旧连接
    // 5. 把 userId 和 session 存起来
    // 6. 把用户写入 Redis → 标记为【在线】
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        //拿到 WebSocket 连接地址
        URI uri = session.getUri();
        if (uri == null) {
            closeSession(session);
            return;
        }

        //拿到地址后的参数，如：?token=xxx
        String query = uri.getQuery();
        if (query == null || !query.startsWith("token=")) {
            closeSession(session);
            return;
        }

        //去掉 token= 这 6 个字符，截取 token
        String token = query.substring(6);
        if (!jwtUtil.validateToken(token)) {
            closeSession(session);
            return;
        }

        //从 token 解析出 用户信息
        //拿到 userId（真正用来标识用户的）
        UserInfo userInfo = jwtUtil.getUserInfoFromToken(token);
        Long userId = userInfo.getId();

        //把新连接存入 sessions
        //把新的 session 放进去（覆盖）
        //返回被覆盖掉的旧 session
        //如果用户已经在线，返回旧连接
        //这一步实现：一个用户只能在线一次
        //如果不存在旧连接，put() 会返回 null
        WebSocketSession old = sessions.put(userId, session);

        //如果存在旧连接
        //先从映射表中删除旧 session
        //关闭旧连接（挤掉之前的登录）
        if (old != null && old.isOpen()) {
            sessionToUser.remove(old.getId());
            try { old.close(); } catch (IOException ignored) {}
        }

        //把当前连接存入映射表
        //sessionId → userId
        //断开连接时靠这个找到用户
        sessionToUser.put(session.getId(), userId);

        // 标记在线（Redis 有序集合，score 为当前时间戳）
        //把用户写入 Redis
        //member = userId
        //score = 当前时间戳（最后活跃时间）
        //表示：这个用户在线了
        stringRedisTemplate.opsForZSet().add(ONLINE_USERS_KEY, String.valueOf(userId), System.currentTimeMillis());

        onlineCount.incrementAndGet();
        log.info("WebSocket 连接建立 userId={}, 当前在线={}", userId, onlineCount.get());
    }

    //客户端断开连接时执行
    // 1. 找到是哪个用户断开了
    // 2. 从连接管理器移除
    // 3. 从 Redis 在线列表删除
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = sessionToUser.remove(session.getId());
        if (userId != null) {
            sessions.remove(userId, session);
            stringRedisTemplate.opsForZSet().remove(ONLINE_USERS_KEY, String.valueOf(userId));
        }
        onlineCount.decrementAndGet();
        log.info("WebSocket 连接关闭 userId={}, 当前在线={}", userId, onlineCount.get());
    }

    //收到客户端消息时执行
    //OnlineTracker.vue每十五秒发送消息更新心跳时间
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            Long userId = sessionToUser.get(session.getId());
            if (userId != null) {
                stringRedisTemplate.opsForZSet().add(ONLINE_USERS_KEY, String.valueOf(userId), System.currentTimeMillis());
            }
        } catch (Exception e) {
            log.warn("心跳处理异常，但不关闭连接", e);
        }
    }

    /**
     * 向指定用户推送消息
     * @param userId 目标用户ID
     * @param message 消息内容
     */
    public void sendToUser(Long userId, TextMessage message) {
        WebSocketSession session = sessions.get(userId);
        if (session == null || !session.isOpen()) return;
        synchronized (session) {
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                log.warn("WebSocket 推送消息失败 userId={}", userId);
            }
        }
    }

    /**
     * 每2秒检查一次在线人数是否有变化，有则广播。
     * 每 10 轮（20 秒）用 Redis 真实值校准一次，防止内存计数器漂移。
     */
    @Scheduled(fixedDelay = 2000)
    public void scheduledBroadcast() {
        broadcastRound++;
        int current;

        // 每 10 轮用 Redis ZCARD 校准内存计数器
        if (broadcastRound % 10 == 0) {
            Long redisCount = stringRedisTemplate.opsForZSet().zCard(ONLINE_USERS_KEY);
            current = redisCount != null ? redisCount.intValue() : onlineCount.get();
            onlineCount.set(current);
        } else {
            current = onlineCount.get();
        }

        //如果人数没变 → 直接跳过，不广播
        if (current == lastBroadcastCount) return;
        lastBroadcastCount = current;

        String message = JSON.toJSONString(Map.of("type", "onlineCount", "count", current));
        TextMessage textMessage = new TextMessage(message);
        for (Long userId : sessions.keySet()) {
            sendToUser(userId, textMessage);
        }
    }

    // OnlineUserTask 清理僵死连接后调用，从 Redis 同步内存计数器
    public void setOnlineCountFromRedis() {
        Long count = stringRedisTemplate.opsForZSet().zCard(ONLINE_USERS_KEY);
        int c = count != null ? count.intValue() : 0;
        onlineCount.set(c);
        lastBroadcastCount = -1; // 强制下次广播
    }

    //注册/删除用户后广播总人数变化
    public void broadcastTotalUsers(int totalUsers) {
        String message = JSON.toJSONString(Map.of("type", "totalUsers", "count", totalUsers));
        TextMessage textMessage = new TextMessage(message);
        for (Long userId : sessions.keySet()) {
            sendToUser(userId, textMessage);
        }
    }

    //关闭长连接
    private void closeSession(WebSocketSession session) {
        try { session.close(); } catch (IOException ignored) {}
    }
}
