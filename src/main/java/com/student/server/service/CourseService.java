package com.student.server.service;

import com.student.server.model.Course;
import com.student.server.model.Result;
import com.student.server.param.PageParam;

import java.util.List;

public interface CourseService {

    Result<List<Course> > findAllCourse();

    Result<List<Course>> findByCondition(String grade, String college, String major, PageParam param);

    Result<Course> addCourse(Course course);

}
