package com.student.server.control;

import com.student.server.model.Result;
import com.student.server.model.User;
import com.student.server.model.UserInfo;
import com.student.server.service.UserService;
import com.student.server.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController /user/login 接口测试
 *
 * <p>使用 @WebMvcTest 只加载 Controller 层，Service 和 JwtUtil 用 Mock 替代，
 * 无需启动真实数据库和 Redis。</p>
 */
@WebMvcTest(UserController.class)
class UserControllerLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ==================== 登录成功 ====================

    @Test
    @DisplayName("登录成功 — 正确的学号和密码")
    void loginSuccess() throws Exception {
        User mockUser = new User();
        mockUser.setId(1);
        mockUser.setStudentNum("2021001");
        mockUser.setUserName("张三");
        mockUser.setNickName("小张");

        Result<User> serviceResult = new Result<>();
        serviceResult.setSuccess(true);
        serviceResult.setCode("200");
        serviceResult.setMessage("登录成功");
        serviceResult.setData(mockUser);

        when(userService.login("2021001", "123456")).thenReturn(serviceResult);
        when(jwtUtil.generateToken(any(UserInfo.class)))
                .thenReturn("mock-jwt-token-xxxxx");

        mockMvc.perform(post("/user/login")
                        .param("studentNum", "2021001")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token-xxxxx"))
                .andExpect(jsonPath("$.data.userInfo.studentNum").value("2021001"))
                .andExpect(jsonPath("$.data.userInfo.userName").value("张三"));
    }

    // ==================== 参数为空 ====================

    @Test
    @DisplayName("登录失败 — 缺少学号参数时返回 400")
    void loginFailWhenStudentNumMissing() throws Exception {
        // @RequestParam 默认 required=true，参数缺失时 Spring MVC 直接返回 400
        mockMvc.perform(post("/user/login")
                        .param("password", "123456"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("登录失败 — 缺少密码参数时返回 400")
    void loginFailWhenPasswordMissing() throws Exception {
        mockMvc.perform(post("/user/login")
                        .param("studentNum", "2021001"))
                .andExpect(status().isBadRequest());
    }

    // ==================== 用户不存在 ====================

    @Test
    @DisplayName("登录失败 — 用户不存在")
    void loginFailWhenUserNotFound() throws Exception {
        Result<User> serviceResult = new Result<>();
        serviceResult.setSuccess(false);
        serviceResult.setCode("601");
        serviceResult.setMessage("登录账户不存在");

        when(userService.login("9999999", "123456")).thenReturn(serviceResult);

        mockMvc.perform(post("/user/login")
                        .param("studentNum", "9999999")
                        .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("601"))
                .andExpect(jsonPath("$.message").value("登录账户不存在"));
    }

    // ==================== 密码错误 ====================

    @Test
    @DisplayName("登录失败 — 密码错误")
    void loginFailWhenWrongPassword() throws Exception {
        Result<User> serviceResult = new Result<>();
        serviceResult.setSuccess(false);
        serviceResult.setCode("603");
        serviceResult.setMessage("登录密码错误");

        when(userService.login("2021001", "wrongPassword"))
                .thenReturn(serviceResult);

        mockMvc.perform(post("/user/login")
                        .param("studentNum", "2021001")
                        .param("password", "wrongPassword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("603"))
                .andExpect(jsonPath("$.message").value("登录密码错误"));
    }
}
