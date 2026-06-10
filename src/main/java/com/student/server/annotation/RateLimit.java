package com.student.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解
 * 只能作用在方法上
 * 注解会保留到程序运行时（生命周期）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 滑动窗口大小（毫秒） */
    long windowMs() default 2000;

    /** 窗口内最大请求数 */
    int maxRequests() default 3;

    /** 限流提示消息 */
    String message() default "操作太频繁，请稍后再试";
}
