package com.student.server.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;


/**
 * 开启 WebSocket 功能，告诉前端：通过 ws://ip:端口/ws/user 这个地址，
 * 就能和后端建立长连接，实现实时消息推送（聊天、通知、在线状态等）。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    //注入消息处理器（真正处理消息的类）
    private final UserWebSocketHandler userWebSocketHandler;

    public WebSocketConfig(UserWebSocketHandler userWebSocketHandler) {
        this.userWebSocketHandler = userWebSocketHandler;
    }

    // 注册 WebSocket 连接地址
    //当前端访问地址：ws://ip:端口/ws/user，建立连接
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                // 绑定处理器 + 访问路径
                .addHandler(userWebSocketHandler, "/ws/user")
                // 允许跨域（前端随便连）
                .setAllowedOrigins("*");
    }
}
