package com.student.server.interceptor;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.server.model.Result;
import com.student.server.model.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
public class UserInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 关键：不自动创建
        HttpSession session = request.getSession(false);

        UserInfo user = null;
        if (session != null) {
            user = (UserInfo) session.getAttribute("user");
        }

        if (user == null) {
            log.error("未登录");
            response.setContentType("application/json;charset=utf-8");
            Result result = new Result();
            result.setSuccess(false);
            result.setMessage("未登录或登录已过期");
            new ObjectMapper().writeValue(response.getWriter(), result);
            return false;
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }
}
