package com.student.server.dao;

import com.student.server.dataobject.CourseDO;
import com.student.server.param.PageParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseDAO {

    int insertCourse(CourseDO courseDO);

    List<CourseDO> findAllCourse();

    List<CourseDO> findPageCourse(@Param("param")PageParam param);

    int countAll(@Param("grade") String grade,@Param("college") String college,@Param("major") String major);

    List<CourseDO> queryByCondition(@Param("grade") String grade,@Param("college") String college,@Param("major") String major,@Param("param") PageParam param);

    CourseDO queryByCourseNum(long courseNum);

    int increasedSelectedCount(@Param("courseId")long courseId);

    int decreasedSelectedCount(@Param("courseId") long courseId);

    CourseDO getByCourseId(long courseId);

}
