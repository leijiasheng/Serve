package com.student.server.service;

import com.student.server.model.Result;
import com.student.server.model.UserCourse;

import java.util.List;

public interface UserCourseService {

    //选课
    Result<UserCourse> insertUserCourse(long userId, long courseId);

    //查询个人选课信息
    Result<List<UserCourse>> selectUserCourseByUserId(long userId);

    //退课
    Result<UserCourse> deleteUserCourse(long userId, long courseId);

}
