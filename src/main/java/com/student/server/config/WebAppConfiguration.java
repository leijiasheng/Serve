package com.student.server.config;

import com.student.server.interceptor.UserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebAppConfiguration implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserInterceptor())
                .addPathPatterns("/**")           // 拦截所有
                .excludePathPatterns("/api/user/login")
                .excludePathPatterns("/user/login")
                .excludePathPatterns("/api/user/register")
                .excludePathPatterns("/user/register")
                .excludePathPatterns("/api/user/forgot")
                .excludePathPatterns("/user/sendCode")
                .excludePathPatterns("/user/forgot");
    }
}
