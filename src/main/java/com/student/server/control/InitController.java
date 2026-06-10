package com.student.server.control;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.server.dao.CommentDAO;
import com.student.server.dao.UserDAO;
import com.student.server.dataobject.UserDO;
import com.student.server.model.Comment;
import com.student.server.model.Result;
import com.student.server.model.User;
import com.student.server.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
@Tag(name = "初始化与工具", description = "数据初始化、查询工具等接口")
public class InitController {

    private final UserDAO userDAO;
    private final CommentDAO commentDAO;
    private final CommentService commentService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping("/init")
    @ResponseBody
    @Operation(summary = "初始化用户数据", description = "从 JSON 文件批量导入用户数据（测试用）")
    public Result<List<UserDO>> initData() {

        Result<List<UserDO>> result = new Result<>();
        result.setSuccess(false);

        InputStream in = UserController.class.getClassLoader().getResourceAsStream("data/userData.json");

        try {
            if (in == null) {
                result.setMessage("用户数据文件未找到");
                return result;
            }

            String content = IOUtils.toString(in, StandardCharsets.UTF_8);

            ObjectMapper objectMapper = new ObjectMapper();
            List<User> users = objectMapper.readValue(content, new TypeReference<List<User>>() {});

            List<UserDO> userDOS = new ArrayList<>();
            users.forEach(user -> {
                UserDO userDO = new UserDO();
                userDO.setUserName(user.getUserName());
                userDO.setNickName(user.getNickName());
                userDO.setEmail(user.getEmail());
                userDO.setStudentNum(user.getStudentNum());

                userDO.setPassword(passwordEncoder.encode(user.getPassword()));
                userDOS.add(userDO);
            });

            userDOS.forEach(userDO -> {
                userDAO.insert(userDO);
            });
            log.info("全部数据插入成功");

            result.setData(userDOS);
            result.setMessage("插入成功");
            result.setSuccess(true);
            return result;
        } catch (IOException e) {
            log.error("初始化用户数据失败", e);
        }

        return result;
    }

    @GetMapping("/findByStudentNum")
    @ResponseBody
    @Operation(summary = "根据学号查询用户", description = "通过学号查询用户信息")
    public Result<UserDO> findByStudentNum(@Parameter(description = "学号") @RequestParam("studentNum") String studentNum) {
        Result<UserDO> result = new Result<>();
        UserDO userDO = userDAO.findByStudentNum(studentNum);

        result.setData(userDO);
        result.setMessage("查询成功");
        return result;
    }

    @GetMapping("/find")
    @ResponseBody
    @Operation(summary = "查询所有评论", description = "获取全部评论数据（原始数据）")
    public List<Comment> findAllComments() {
        return commentDAO.findAllComments();
    }

    @GetMapping("/comments")
    @ResponseBody
    @Operation(summary = "获取评论列表（带分页）")
    public Result<List<Comment>> commentList() {
        Result<List<Comment>> result = commentService.findAllComments();
        return result;
    }

    @GetMapping("/getCookies")
    @ResponseBody
    @Operation(summary = "获取 Cookies", description = "查看当前请求的 Cookie 信息")
    public Map<String, Object> index(HttpServletRequest request) {
        Map<String, Object> returnData = new HashMap<>();
        returnData.put("result", "this is song list");

        Cookie[] cookies = request.getCookies();
        returnData.put("cookies", cookies);

        return returnData;
    }
}


