package com.student.server.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.server.model.Result;
import com.student.server.model.UserInfo;
import com.student.server.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@RequiredArgsConstructor
@Slf4j
@Component
public class UserInterceptor implements HandlerInterceptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //允许跨域请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                return true;
            }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error("未提供有效的认证令牌");
            response.setContentType("application/json;charset=utf-8");
            Result result = new Result();
            result.setSuccess(false);
            result.setMessage("未登录或登录已过期");
            response.setStatus(401);
            OBJECT_MAPPER.writeValue(response.getWriter(), result);
            return false;
        }

        //把 Bearer 这 7 个字符去掉，拿到纯token
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            log.error("令牌无效或已过期");
            response.setContentType("application/json;charset=utf-8");
            Result result = new Result();
            result.setSuccess(false);
            result.setMessage("未登录或登录已过期");
            response.setStatus(401);
            OBJECT_MAPPER.writeValue(response.getWriter(), result);
            return false;
        }

        UserInfo userInfo = jwtUtil.getUserInfoFromToken(token);
        request.setAttribute("currentUser", userInfo);
        return true;
    }
}
