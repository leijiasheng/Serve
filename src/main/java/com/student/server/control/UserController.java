package com.student.server.control;

import com.student.server.annotation.RateLimit;
import com.student.server.model.LoginVO;
import com.student.server.model.Result;
import com.student.server.model.User;
import com.student.server.model.UserInfo;
import com.student.server.param.PageParam;
import com.student.server.service.UserService;
import com.student.server.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "用户管理", description = "用户注册、登录、信息管理、密码修改、头像上传等接口")
public class UserController {

    private final UserService userService;

    private final JwtUtil jwtUtil;

    @Value("${upload.dir:${user.dir}}")
    private String uploadDir;

    @GetMapping("/person")
    @Operation(summary = "获取当前用户信息", description = "直接从redis/数据库获取当前登录用户的详细信息")
    public Result<UserInfo> person(HttpServletRequest request) {
        UserInfo currentUser = (UserInfo) request.getAttribute("currentUser");
        Result<UserInfo> result = new Result<>();

        if (currentUser == null) {
            result.setSuccess(false);
            result.setMessage("未登录");
            return result;
        }

        // 从 Redis/DB 获取最新数据，避免 JWT 中的旧数据
        Result<User> fresh = userService.findByStudentNum(currentUser.getStudentNum());
        if (fresh.isSuccess() && fresh.getData() != null) {
            UserInfo userInfo = new UserInfo();
            BeanUtils.copyProperties(fresh.getData(), userInfo);
            result.setData(userInfo);
        } else {
            result.setData(currentUser);
        }

        log.info("进入个人中心，学号：{}", currentUser.getStudentNum());
        result.setCode("200");
        result.setMessage("已登录");
        result.setSuccess(true);
        return result;
    }

    @RateLimit(windowMs = 60000, maxRequests = 5, message = "注册过于频繁，请稍后再试")
    @PostMapping("/user/register")
    @Operation(summary = "用户注册", description = "通过用户名、密码、学号、邮箱和验证码进行注册")
    public Result<User> register(@Parameter(description = "用户名") @RequestParam("userName") String userName,
                                 @Parameter(description = "密码") @RequestParam("password") String password,
                                 @Parameter(description = "学号") @RequestParam("studentNum") String studentNum,
                                 @Parameter(description = "邮箱") @RequestParam("email") String email,
                                 @Parameter(description = "验证码") @RequestParam("code") String code) {
       return userService.register(userName, password, email, studentNum, code);
    }

    @RateLimit(windowMs = 60000, maxRequests = 5, message = "登录过于频繁，请稍后再试")
    @PostMapping("/user/login")
    @ResponseBody
    @Operation(summary = "用户登录", description = "通过学号和密码登录，返回 JWT Token")
    public Result<LoginVO> login(@Parameter(description = "学号") @RequestParam("studentNum") String studentNum,
                                 @Parameter(description = "密码") @RequestParam("password") String password) {

        Result<User> result = userService.login(studentNum, password);

        if (!result.isSuccess()) {
            Result<LoginVO> res = new Result<>();
            res.setSuccess(false);
            res.setMessage(result.getMessage());
            res.setCode(result.getCode());
            res.setData(null);
            log.warn("登陆失败，学号：{}", studentNum);
            return res;
        }

        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(result.getData(), userInfo);

        String token = jwtUtil.generateToken(userInfo);

        log.info("登陆成功，学号：{}", studentNum);

        Result<LoginVO> res = new Result<>();
        res.setSuccess(true);
        res.setMessage("登录成功");
        res.setCode("200");
        res.setData(new LoginVO(token, userInfo));

        return res;
    }

    @PostMapping("/user/logout")
    @Operation(summary = "用户退出登录")
    public Result<UserInfo> logout() {
        Result<UserInfo> res = new Result<>();
        res.setSuccess(true);
        res.setMessage("退出登录成功");
        log.info("退出登录成功");
        return res;
    }

    @RateLimit(windowMs = 60000, maxRequests = 2, message = "操作过于频繁，请稍后再试")
    @PostMapping("/user/forgot")
    @Operation(summary = "忘记密码", description = "通过学号、邮箱和验证码重置密码")
    public Result<User> resetPwd(@Parameter(description = "学号") @RequestParam("studentNum") String studentNum,
                                 @Parameter(description = "邮箱") @RequestParam("email") String email,
                                 @Parameter(description = "验证码") @RequestParam("code") String code) {
        Result<User> result = userService.resetPwd(studentNum, email, code);
        log.info("重置密码结果：isSuccess={}, message={}", result.isSuccess(), result.getMessage());
        return result;
    }

    @RateLimit(windowMs = 60000, maxRequests = 3, message = "操作过于频繁，请稍后再试")
    @PostMapping("/user/update")
    @Operation(summary = "更新个人信息", description = "修改昵称、邮箱和个人签名")
    public Result<User> updatePerMsg(@Parameter(description = "昵称") @RequestParam("nickName") String nickName,
                                     @Parameter(description = "学号") @RequestParam("studentNum") String studentNum,
                                     @Parameter(description = "邮箱") @RequestParam("email") String email,
                                     @Parameter(description = "个人签名") @RequestParam("personSign") String personSign,
                                     HttpServletRequest request) {
        Result<User> result = userService.updatePerMsg(nickName, studentNum, email, personSign);
        if (result.isSuccess()) {
            log.info("更新个人信息成功：学号：{}", studentNum);
            return result;
        } else {
            log.warn("更新个人信息失败，学号：{}", studentNum);
            return result;
        }
    }

    @RateLimit(windowMs = 60000, maxRequests = 2, message = "操作过于频繁，请稍后再试")
    @PostMapping("/handleUpdatePwd")
    @Operation(summary = "修改密码", description = "提供旧密码和新密码进行密码修改（需登录）")
    public Result<User> handleUpdatePwd(@Parameter(description = "旧密码") @RequestParam("oldPwd") String oldPwd,
                                        @Parameter(description = "新密码") @RequestParam("newPwd") String newPwd,
                                        HttpServletRequest request) {

        UserInfo currentUser = (UserInfo) request.getAttribute("currentUser");
        if (currentUser == null) {
            Result<User> res = new Result<>();
            res.setSuccess(false);
            res.setMessage("未登录");
            return res;
        }

        Result<User> result = userService.updatePwd(oldPwd, newPwd, currentUser.getStudentNum());

        if (result.isSuccess()) {
            log.info("修改密码成功，学号：{}", currentUser.getStudentNum());
            return result;
        } else {
            log.warn("修改密码失败，学号：{}", currentUser.getStudentNum());
            return result;
        }
    }

    @RateLimit(windowMs = 60000, maxRequests = 5, message = "操作过于频繁，请稍后再试")
    @PostMapping("/user/uploadAvatar")
    @Operation(summary = "上传头像", description = "上传 JPG/PNG/GIF 格式头像，大小不超过 2MB（需登录）")
    public Result<UserInfo> uploadAvatar(@Parameter(description = "头像文件") @RequestParam("file") MultipartFile file,
                                         HttpServletRequest request) {

        Result<UserInfo> result = new Result<>();

        UserInfo currentUser = (UserInfo) request.getAttribute("currentUser");
        if (currentUser == null) {
            result.setSuccess(false);
            result.setMessage("未登录");
            return result;
        }

        if (file.isEmpty()) {
            result.setSuccess(false);
            result.setMessage("请选择头像文件");
            return result;
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/gif"))) {
            result.setSuccess(false);
            result.setMessage("仅支持 JPG、PNG、GIF 格式的图片");
            return result;
        }

        if (file.getSize() > 2 * 1024 * 1024) {
            result.setSuccess(false);
            result.setMessage("头像大小不能超过 2MB");
            return result;
        }

        try {
            String uploadDir = this.uploadDir + "/uploads/avatar/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String fileName = currentUser.getStudentNum() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            String filePath = uploadDir + fileName;
            file.transferTo(new File(filePath));

            String avatarUrl = "/uploads/avatar/" + fileName;
            Result<User> updateResult = userService.updateAvatar(currentUser.getStudentNum(), avatarUrl);

            if (updateResult.isSuccess()) {
                currentUser.setAvatar(avatarUrl);
                result.setSuccess(true);
                result.setMessage("头像上传成功");
                result.setCode("200");
                result.setData(currentUser);
                log.info("头像上传成功，学号：{}", currentUser.getStudentNum());
            } else {
                result.setSuccess(false);
                result.setMessage(updateResult.getMessage());
            }
        } catch (IOException e) {
            log.error("头像上传失败", e);
            result.setSuccess(false);
            result.setMessage("头像上传失败，请稍后重试");
        }

        return result;
    }

    @GetMapping("/user/userList")
    @Operation(summary = "获取用户列表", description = "分页查询用户列表，可按关键字搜索")
    public Result<List<User>> userList(@Parameter(description = "关键字（可选）") @RequestParam(required = false) String keyWord,
                                       @Parameter(hidden = true) PageParam param) {

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
    @Operation(summary = "添加用户", description = "管理员添加新用户")
    public Result<User> insertUser(@RequestBody User user) {
        log.info("添加用户");
        return userService.insertUser(user);
    }

    @PostMapping("/user/delete")
    @Operation(summary = "删除用户", description = "根据学号删除用户")
    public Result<User> delete(@Parameter(description = "学号") @RequestParam("studentNum") String studentNum) {

        Result<User> result = userService.deleteByStudentNum(studentNum);

        if (result.isSuccess()) {
            log.info("删除用户成功，学号：{}", studentNum);
            return result;
        } else {
            log.info("删除用户失败，学号：{}", studentNum);
            return result;
        }
    }

    @PostMapping("/user/sendCode")
    @Operation(summary = "发送重置密码验证码", description = "向指定邮箱发送重置密码的验证码")
    public Result<String> sendCode(@Parameter(description = "学号") String studentNum,
                                   @Parameter(description = "邮箱") String email) {
        log.info("发送验证码");
        return userService.buildCode(studentNum, email);
    }

    @RateLimit(windowMs = 60000, maxRequests = 1, message = "验证码已发送，请稍后再试")
    @PostMapping("/user/sendRegCode")
    @Operation(summary = "发送注册验证码", description = "向指定邮箱发送注册验证码")
    public Result<String> sendRegCode(@Parameter(description = "学号") String studentNum,
                                      @Parameter(description = "邮箱") String email) {
        return userService.buildRegCode(studentNum, email);
    }
}
