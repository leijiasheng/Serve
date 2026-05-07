package com.student.server.dataobject;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.student.server.model.Course;
import com.student.server.model.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class UserDO implements Serializable {

    private long id;

    private String userName;

    private String nickName;

    private String password;

    private String email;

    private String studentNum;

    private String personSign;

    private String avatar;

    private List<Course> courseList;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreated;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtModified;

    public User toModel() {
        User user = new User();
        user.setUserName(userName);
        user.setId(id);
        user.setEmail(email);
//        user.setPassword(password);
        user.setAvatar(avatar);
        user.setNickName(nickName);
        user.setStudentNum(studentNum);
        user.setPersonSign(personSign);
        user.setGmtCreated(getGmtCreated());
        user.setGmtModified(gmtModified);
        return user;
    }
}
