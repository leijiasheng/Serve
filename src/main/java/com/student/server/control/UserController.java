package com.student.server.control;

import com.student.server.model.Result;
import com.student.server.model.User;
import com.student.server.model.UserInfo;
import com.student.server.param.PageParam;
import com.student.server.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 根据登陆状态查询个人信息
     * @param request
     * @return
     */
    @GetMapping("/person")
    public Result<UserInfo> person(HttpServletRequest request) {
        HttpSession session = request.getSession(false); // 不自动创建新session
        Result<UserInfo> result = new Result<>();

        // 修复：先判断 session 是否存在
        if (session == null) {
            result.setSuccess(false);
            result.setMessage("未登录");
            return result;
        }

        UserInfo user = (UserInfo) session.getAttribute("user");

        if (user != null) {
            log.info("进入个人中心");
            result.setCode("200");
            result.setMessage("已登录");
            result.setSuccess(true);
            result.setData(user);
        } else {
            log.warn("未进入个人中心");
            result.setSuccess(false);
            result.setMessage("未登录");
        }
        return result;
    }

    /**
     * 注册账户
     * @param userName
     * @param password
     * @param studentNum
     * @param email
     * @return
     */
    @PostMapping("/user/register")
    public Result<User> register(@RequestParam("userName") String userName,@RequestParam("password") String password, @RequestParam("studentNum") String studentNum,@RequestParam("email") String email) {

       return userService.register(userName, password, email, studentNum);

//        if (result.isSuccess()) {
//            log.info("注册成功，学号：{}", studentNum);
//            Result<User> res = new Result<>();
//            res.setSuccess(true);
//            res.setMessage("注册成功");
//            res.setCode("200");
//            res.setData(result.getData());
//            return res;
//        } else {
//            log.warn("注册失败，学号：{}", studentNum);
//            Result<User> res = new Result<>();
//            res.setSuccess(false);
//            res.setMessage(result.getMessage());
//            res.setCode(result.getCode());
//            res.setData(null);
//            return res;
//        }

    }

    /**
     * 登录账户
     * @param studentNum
     * @param password
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/user/login")
    @ResponseBody
    public Result<UserInfo> login(@RequestParam("studentNum") String studentNum, @RequestParam("password") String password,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {

        Result<User> result = userService.login(studentNum, password);

        // 登录失败
        if (!result.isSuccess()) {
            Result<UserInfo> res = new Result<>();
            res.setSuccess(false);
            res.setMessage(result.getMessage());
            res.setCode(result.getCode());
            res.setData(null);
            log.warn("登陆失败，学号：{}", studentNum);
            return res;
        }

        // 登录成功后
        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(result.getData(), userInfo);

        // 强制销毁旧会话，创建全新会话
        request.getSession().invalidate(); // 销毁旧的


        String redisKey = "user:sessionId:" + studentNum;

        String sessionId =(String) redisTemplate.opsForValue().get(redisKey);
        redisTemplate.delete("spring:session:" + sessionId);
        redisTemplate.delete("spring:session:sessions:expires:" + sessionId);
        redisTemplate.delete(redisKey);

        HttpSession session = request.getSession(true); // 新建

        // 存入用户信息
        session.setAttribute("user", userInfo);

        // 存入 Redis 记录当前用户的 sessionId
        String newRedisKey = "user:sessionId:" + studentNum;
        redisTemplate.opsForValue().set(newRedisKey, session.getId(), 6, TimeUnit.HOURS);
        log.info("登陆成功，学号：{}", studentNum);

        // 封装返回
        Result<UserInfo> res = new Result<>();
        res.setSuccess(true);
        res.setMessage("登录成功");
        res.setCode("200");
        res.setData(userInfo);

        return res;
    }

    /**
     * 退出登录
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/user/logout")
    public Result<UserInfo> logout(HttpServletRequest request,
                                   HttpServletResponse response) {

        Result<UserInfo> res = new Result<>();
        res.setSuccess(true);

        //不创建新的session
        HttpSession session = request.getSession(false);

        if (session != null) {

            UserInfo userInfo = (UserInfo) session.getAttribute("user");

            String sessionId= session.getId();
            //销毁会话
            session.invalidate();


            if (userInfo.getStudentNum() != null) {
                String studentNum = userInfo.getStudentNum();

                String redisKey = "user:sessionId:" + studentNum;

                redisTemplate.delete("spring:session:" + sessionId);
                redisTemplate.delete("spring:session:sessions:expires:" + sessionId);
                redisTemplate.delete(redisKey);
                redisTemplate.delete(studentNum);

                res.setMessage("退出登录成功");
                log.info("退出登录成功，学号：{}, sessionId: {}", studentNum, sessionId);
            }

            Cookie cookie = new Cookie("JSESSIONID", null);
            cookie.setPath("/");
            cookie.setMaxAge(0);

            response.addCookie(cookie);

            return res;
        }

        res.setSuccess(false);
        res.setMessage("退出登录失败");
        return res;
    }

    /**
     * 忘记密码
     * @param studentNum
     * @param email
     * @return
     */
    @PostMapping("/user/forgot")
    public Result<User> resetPwd(@RequestParam("studentNum") String studentNum,@RequestParam("email") String email,@RequestParam("code") String code) {
        Result<User> result = userService.resetPwd(studentNum, email, code);
        log.info("重置密码结果：isSuccess={}, message={}", result.isSuccess(), result.getMessage());
        return result;
    }

    /**
     * 修改个人信息
     * @param nickName
     * @param studentNum
     * @param email
     * @param personSign
     * @param request
     * @return
     */
    @PostMapping("/user/update")
    public Result<User> updatePerMsg(@RequestParam("nickName") String nickName,@RequestParam("studentNum") String studentNum,@RequestParam("email") String email,@RequestParam("personSign") String personSign,
                                     HttpServletRequest request) {
        Result<User> result = userService.updatePerMsg(nickName, studentNum, email, personSign);

        if (result.isSuccess()) {
            UserInfo userInfo = new UserInfo();
            BeanUtils.copyProperties(result.getData(), userInfo);
            request.getSession().setAttribute("user", userInfo);
            log.info("更新个人信息成功：学号：{}", studentNum);
            return result;
        } else {
            log.warn("更新个人信息失败，学号：{}", studentNum);
            return result;
        }
    }

    /**
     * 修改密码
     * @param oldPwd
     * @param newPwd
     * @param request
     * @return
     */
    @PostMapping("/handleUpdatePwd")
    public Result<User> handleUpdatePwd(@RequestParam("oldPwd") String oldPwd, @RequestParam("newPwd") String newPwd,
                                        HttpServletRequest request) {

        UserInfo user = (UserInfo) request.getSession().getAttribute("user");

        Result<User> result = userService.updatePwd(oldPwd, newPwd, user.getStudentNum());

        if (result.isSuccess()) {
            log.info("修改密码成功，学号：{}", user.getStudentNum());
            return result;
        } else {
            log.warn("修改密码失败，学号：{}", user.getStudentNum());
            return result;
        }
    }

    /**
     * 查询用户列表
     * @param keyWord
     * @param param
     * @return
     */
    @GetMapping("/user/userList")
    public Result<List<User>> userList(@RequestParam(required = false) String keyWord,PageParam param) {

        Result<List<User>> result = userService.findAll(param);
        int total = userService.countAll();

        if (keyWord != null) {
            result = userService.findByKeyWord(keyWord, param);
            total = userService.countByKeyWord(keyWord);
        }

        int currentPage = param.getPageNum();
        int pageSize = param.getPageSize();
        int totalPage = (total + pageSize - 1) / pageSize;

        result.setPageSize(pageSize);
        result.setTotalCount(total);
        result.setPageNum(currentPage);
        result.setTotalPage(totalPage);

        log.info("进入用户列表");

        return result;
    }

    @PostMapping("/user/insert")
    public Result<User> insertUser(@RequestBody User user) {
        log.info("添加用户");
        return userService.insertUser(user);
    }

    /**
     * 删除用户
     * @param studentNum
     * @param response
     * @param request
     * @return
     */
    @PostMapping("/user/delete")
    public Result<User> delete(@RequestParam("studentNum") String studentNum,
                               HttpServletResponse response,
                               HttpServletRequest request) {

        Result<User> result = userService.deleteByStudentNum(studentNum);

        if (result.isSuccess()) {

            String sessionId =(String) redisTemplate.opsForValue().get("user:sessionId:" + studentNum);
            if (sessionId != null) {

                redisTemplate.delete("spring:session:" + sessionId);
                redisTemplate.delete("spring:session:sessions:expires:" + sessionId);
                redisTemplate.delete("user:sessionId:" + studentNum);

                log.info("已强制删除用户的SESSION：{}", studentNum);
            }
            // 核心：只有【删除自己】时，才销毁自己的session + 清Cookie
            UserInfo currentUser = (UserInfo) request.getSession().getAttribute("user");
            if (currentUser != null && currentUser.getStudentNum().equals(studentNum)) {
                // 删自己 → 销毁session
                request.getSession().invalidate();

                // 删自己 → 清除浏览器Cookie
                Cookie cookie = new Cookie("JSESSIONID", null);
                cookie.setPath("/");
                cookie.setMaxAge(0);
                response.addCookie(cookie);

                log.info("删除自己，强制退出登录");
            }

            log.info("删除用户成功，学号：{}", studentNum);
            return result;
        } else {
            log.info("删除用户失败，学号：{}", studentNum);
            return result;
        }
    }

    /**
     * 发送验证码，用于重置密码
     * @param studentNum
     * @param email
     * @return
     */
    @PostMapping("/user/sendCode")
    public Result<String> sendCode(String studentNum, String email) {
        return userService.buildCode(studentNum, email);
    }
}

