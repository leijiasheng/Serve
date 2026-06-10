package com.student.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;


//移除 @EnableRedissonHttpSession，不再使用 Redis 会话存储
//@EnableRedisHttpSession
@Configuration
public class SpringHttpSessionConfig {

}
