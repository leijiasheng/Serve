package com.student.server.exception;

import com.student.server.model.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
//@RestControllerAdvice 全局捕获 RateLimitException，返回 HTTP 429 + JSON
@RestControllerAdvice
public class GlobalExceptionHandler {

    //只专门处理限流异常
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Result<Void>> handleRateLimit(RateLimitException e) {
        log.warn("RateLimitException: {}", e.getMessage());
        Result<Void> result = new Result<>();
        result.setSuccess(false);
        result.setMessage(e.getMessage());
        return ResponseEntity.status(429).body(result);
    }
}
