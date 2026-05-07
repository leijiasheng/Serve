package com.student.server.dao;

import com.student.server.dataobject.UserCourseDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserCourseDAO {

    int insertUserCourse(@Param("userId") long userId,@Param("courseId") long courseId);

   List<UserCourseDO > selectUserCourseByUserId(@Param("userId") long userId);

   int deleteUserCourse(@Param("userId") long userId,@Param("courseId") long courseId);

   UserCourseDO getByUserIdAndCourseId(@Param("userId") long userId,@Param("courseId") long courseId);

   int countSelectedCourse(long userId);
}
