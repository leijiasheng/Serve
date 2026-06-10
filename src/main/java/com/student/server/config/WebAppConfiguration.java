package com.student.server.config;

import com.student.server.interceptor.RateLimiterInterceptor;
import com.student.server.interceptor.UserInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
@Configuration
public class WebAppConfiguration implements WebMvcConfigurer {

    private final UserInterceptor userInterceptor;

    private final RateLimiterInterceptor rateLimiterInterceptor;

    @Value("${upload.dir:${user.dir}}")
    private String uploadDir;

    //登录拦截
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/user/login")
                .excludePathPatterns("/user/login")
                .excludePathPatterns("/api/user/register")
                .excludePathPatterns("/user/register")
                .excludePathPatterns("/api/user/forgot")
                .excludePathPatterns("/user/sendCode")
                .excludePathPatterns("/user/sendRegCode")
                .excludePathPatterns("/user/forgot")
                .excludePathPatterns("/uploads/**")
                .excludePathPatterns("/swagger-ui.html")
                .excludePathPatterns("/swagger-ui/**")
                .excludePathPatterns("/v3/api-docs/**")
                .excludePathPatterns("/api-docs/**")
                .excludePathPatterns("/webjars/**")
                .excludePathPatterns("/ws/**")
                .excludePathPatterns("/init");

        //自定义接口限流拦截器
        //抢购接口滑动窗口限流（基于 Redis Sorted Set）
        registry.addInterceptor(rateLimiterInterceptor)
                .addPathPatterns("/api/product/snappedUp");
    }

    //让浏览器可以通过 URL 访问你项目里上传的图片、文件
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/uploads/");
    }
}
