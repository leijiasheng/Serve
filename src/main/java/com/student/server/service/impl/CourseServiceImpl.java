package com.student.server.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.esotericsoftware.kryo.util.ObjectMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.server.dao.CourseDAO;
import com.student.server.dao.UserCourseDAO;
import com.student.server.dataobject.CourseDO;
import com.student.server.model.Course;
import com.student.server.model.Result;
import com.student.server.param.PageParam;
import com.student.server.service.CourseService;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.beans.BeanUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseDAO courseDAO;

    private final RedisTemplate redisTemplate;

    /**
     *全量查询
     */
    @Override
    public Result<List<Course>> findAllCourse() {
        Result<List<Course>> result = new Result<>();
        result.setSuccess(true);
        List<CourseDO> courseDOS = courseDAO.findAllCourse();
        if (CollectionUtils.isEmpty(courseDOS)) {
            result.setSuccess(false);
            result.setMessage("暂无课程");
            return result;
        }

        List<Course> courses = courseDOS.stream().map(CourseDO::toModel).collect(Collectors.toList());


        result.setMessage("查找成功");
        result.setCode("200");
        result.setData(courses);
        return result;
    }

    /**
     *条件查询或者全量分页查询
     */
    @Override
    public Result<List<Course>> findByCondition(String grade, String college, String major, PageParam param) {
        Result<List<Course>> result = new Result<>();
        result.setSuccess(true);

        String hashKey = "course:all";

        Map<Object, Object> redisMap = redisTemplate.opsForHash().entries(hashKey);
        List<Course> allCourses;

        if (redisMap.isEmpty()) {
            allCourses = courseDAO.findAllCourse().stream().map(CourseDO::toModel).collect(Collectors.toList());

            Map<String, String> courseMap = allCourses.stream()
                    .collect(Collectors.toMap(course -> String.valueOf(course.getId()), JSON::toJSONString));

            redisTemplate.opsForHash().putAll(hashKey, courseMap);
            redisTemplate.expire(hashKey, 3, TimeUnit.HOURS);
        } else {
            allCourses = redisMap.values().stream()
                    .map(obj -> JSON.parseObject(obj.toString(), Course.class))
                    .collect(Collectors.toList());
        }

        List<Course> filtered = allCourses.stream()
                .filter(c -> {
                    boolean match = true;
                    if (StringUtils.hasText(grade)) {
                        match &= c.getGrade() != null && c.getGrade().contains(grade);
                    }
                    if (StringUtils.hasText(college)) {
                        match &= c.getCollege() != null && c.getCollege().contains(college);
                    }
                    if (StringUtils.hasText(major)) {
                        match &= c.getMajor() != null && c.getMajor().contains(major);
                    }
                    return match;
                })
                .collect(Collectors.toList());

        int totalCount = filtered.size();
        int pageNum = param.getPageNum();
        int pageSize = param.getPageSize();

        List<Course> pageData = filtered.stream()
                .skip((long) (pageNum - 1) * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());

        result.setData(pageData);
        result.setTotalCount(totalCount);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotalPage((totalCount + pageSize - 1) / pageSize);
        result.setMessage("查询课程成功");
        result.setCode("200");
        return result;
    }

    @Override
    public Result<Course> addCourse(Course course) {
        Result<Course> result = new Result<>();
        CourseDO courseDO = new CourseDO();
        BeanUtils.copyProperties(course, courseDO);
        int insertRes = courseDAO.insertCourse(courseDO);
        if (insertRes <= 0) {
            result.setSuccess(false);
            result.setMessage("添加课程失败");
            return result;
        } else {
            result.setSuccess(true);
            result.setMessage("添加课程成功");
            result.setData(course);
            return result;
        }
    }
}
