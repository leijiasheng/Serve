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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
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
@Slf4j
public class InitController {

    @Autowired
    private UserDAO userDAO;
    @Autowired
    private CommentDAO commentDAO;
    @Autowired
    private CommentService commentService;

    @GetMapping("/init")
    @ResponseBody
    public Result<List<UserDO>> initData() {

        Result<List<UserDO>> result = new Result<>();
        result.setSuccess(false);

        InputStream in = UserController.class.getClassLoader().getResourceAsStream("data/userData.json");

        try {
            assert in != null;

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
                userDO.setPassword(user.getPassword());

                String saltPwd = user.getPassword() + "ljs_zwy";
                String md5Pwd = DigestUtils.md5DigestAsHex(saltPwd.getBytes()).toUpperCase();

                userDO.setPassword(md5Pwd);
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
            e.printStackTrace();
        }

        return result;
    }

    @GetMapping("/findByStudentNum")
    @ResponseBody
    public Result<UserDO> findByStudentNum(@RequestParam("studentNum") String studentNum) {
        Result<UserDO> result = new Result<>();
        UserDO userDO = userDAO.findByStudentNum(studentNum);

        result.setData(userDO);
        result.setMessage("查询成功");
        return result;
    }

    @GetMapping("/find")
    @ResponseBody
    public List<Comment> findAllComments() {
        return commentDAO.findAllComments();
    }

    @GetMapping("/comments")
    @ResponseBody
    public Result<List<Comment>> commentList() {
        Result<List<Comment>> result = commentService.findAllComments();
        return result;
    }

    @GetMapping("/getCookies")
    @ResponseBody
    public Map index(HttpServletRequest request) {
        Map returnData = new HashMap();
        returnData.put("result", "this is song list");

        Cookie[] cookies = request.getCookies();
        returnData.put("cookies", cookies);

        return returnData;
    }
}


