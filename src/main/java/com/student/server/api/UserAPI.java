package com.student.server.api;

import com.student.server.model.Result;
import com.student.server.model.User;
import com.student.server.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserAPI {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<User> register(@RequestParam("userName") String userName, @RequestParam("password") String password,
                           @RequestParam("studentNum") String studentNum, @RequestParam("email") String email) {
        return userService.register(userName, password, email, studentNum);
    }

    @PostMapping("/login")
    public Result<User> login(@RequestParam("studentNum") String studentNum,
                        @RequestParam("password") String password) {
       return userService.login(studentNum, password);

    }

    @GetMapping("/logout")
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @PostMapping("/forgot")
    public Result<User> resetPwd(@RequestParam("studentNum") String studentNum,@RequestParam("email") String email,@RequestParam("code") String code) {
        return userService.resetPwd(studentNum, email, code);
    }
}
