package com.student.server.exception;
//运行时异常，携带限流 message
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
