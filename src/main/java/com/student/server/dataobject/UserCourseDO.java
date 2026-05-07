package com.student.server.dataobject;

import com.student.server.dao.CourseDAO;
import com.student.server.model.Course;
import com.student.server.model.User;
import com.student.server.model.UserCourse;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class UserCourseDO {

    private long id;

    private long userId;

    private long courseId;

    private User user;

    private Course course;

    public UserCourse toModel() {
        UserCourse userCourse = new UserCourse();
        BeanUtils.copyProperties(this, userCourse);
        return userCourse;
    }
}
