package com.student.server.api;

import com.student.server.annotation.RateLimit;
import com.student.server.model.LoginVO;
import com.student.server.model.Result;
import com.student.server.model.User;
import com.student.server.model.UserInfo;
import com.student.server.service.UserService;
import com.student.server.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户 API", description = "用户注册、登录、退出、忘记密码等移动端 API 接口")
public class UserAPI {

    private final UserService userService;

    private final JwtUtil jwtUtil;

    @RateLimit(windowMs = 60000, maxRequests = 5, message = "注册过于频繁，请稍后再试")
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "通过用户名、密码、学号、邮箱和验证码进行注册（移动端）")
    public Result<User> register(@Parameter(description = "用户名") @RequestParam("userName") String userName,
                                 @Parameter(description = "密码") @RequestParam("password") String password,
                                 @Parameter(description = "学号") @RequestParam("studentNum") String studentNum,
                                 @Parameter(description = "邮箱") @RequestParam("email") String email,
                                 @Parameter(description = "验证码") @RequestParam("code") String code) {
        return userService.register(userName, password, email, studentNum, code);
    }

    @RateLimit(windowMs = 60000, maxRequests = 5, message = "登录过于频繁，请稍后再试")
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过学号和密码登录，返回 JWT Token（移动端）")
    public Result<LoginVO> login(@Parameter(description = "学号") @RequestParam("studentNum") String studentNum,
                                 @Parameter(description = "密码") @RequestParam("password") String password) {
        Result<User> result = userService.login(studentNum, password);

        if (!result.isSuccess()) {
            Result<LoginVO> res = new Result<>();
            res.setSuccess(false);
            res.setMessage(result.getMessage());
            res.setCode(result.getCode());
            res.setData(null);
            return res;
        }

        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(result.getData(), userInfo);
        String token = jwtUtil.generateToken(userInfo);

        Result<LoginVO> res = new Result<>();
        res.setSuccess(true);
        res.setMessage("登录成功");
        res.setCode("200");
        res.setData(new LoginVO(token, userInfo));
        return res;
    }

    @GetMapping("/logout")
    @Operation(summary = "退出登录", description = "用户退出登录（移动端）")
    public Result<UserInfo> logout() {
        Result<UserInfo> res = new Result<>();
        res.setSuccess(true);
        res.setMessage("退出登录成功");
        return res;
    }

    @RateLimit(windowMs = 60000, maxRequests = 3, message = "操作过于频繁，请稍后再试")
    @PostMapping("/forgot")
    @Operation(summary = "忘记密码", description = "通过学号、邮箱和验证码重置密码（移动端）")
    public Result<User> resetPwd(@Parameter(description = "学号") @RequestParam("studentNum") String studentNum,
                                 @Parameter(description = "邮箱") @RequestParam("email") String email,
                                 @Parameter(description = "验证码") @RequestParam("code") String code) {
        return userService.resetPwd(studentNum, email, code);
    }
}
