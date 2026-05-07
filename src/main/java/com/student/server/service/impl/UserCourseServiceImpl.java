package com.student.server.service.impl;

import com.alibaba.fastjson2.JSON;
import com.student.server.dao.CourseDAO;
import com.student.server.dao.UserCourseDAO;
import com.student.server.dataobject.CourseDO;
import com.student.server.dataobject.UserCourseDO;
import com.student.server.model.*;
import com.student.server.redisKeys.RedisConstant;
import com.student.server.service.UserCourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
;
import java.util.stream.Collectors;

@Service
public class UserCourseServiceImpl implements UserCourseService {

    @Autowired
    private UserCourseDAO userCourseDAO;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CourseDAO courseDAO;

    /**
     * 选课，并更新数据库和缓存redis
     * @param userId
     * @param courseId
     * @return
     */
    @Override
    public Result<UserCourse> insertUserCourse(long userId, long courseId) {
        Result<UserCourse> result = new Result<>();
        result.setSuccess(true);

        UserCourseDO exist = userCourseDAO.getByUserIdAndCourseId(userId, courseId);
        if (exist != null) {
            result.setSuccess(false);
            result.setMessage("你已选过该课程，不可重复选课");
            return result;
        }

        CourseDO courseDO = courseDAO.getByCourseId(courseId);

        if (courseDO == null) {
            result.setSuccess(false);
            result.setMessage("课程不存在");
            return result;
        }

        if (courseDO.getSelectedCount() >= courseDO.getTotalCapacity()) {
            result.setSuccess(false);
            result.setMessage("课程已满，选课失败");
            return result;
        }

        int insertRes = userCourseDAO.insertUserCourse(userId, courseId);
        if (insertRes <= 0) {
            result.setSuccess(false);
            result.setMessage("选课失败");
            return result;
        }

        int updateRows = courseDAO.increasedSelectedCount(courseId);
        if (updateRows <= 0) {
            result.setSuccess(false);
            result.setMessage("选课成功，但更新人数失败，请联系管理员");
            return result;
        }

        String field = String.valueOf(courseId);
        Object courseObj = redisTemplate.opsForHash().get(RedisConstant.COURSE_HASH_KEY, field);

        if (courseObj != null) {
            Course course = JSON.parseObject(courseObj.toString(), Course.class);
            course.setSelectedCount(course.getSelectedCount() + 1);
            course.setRemaining(course.getRemaining() - 1);

            String userFieldId = String.valueOf(userId);
            //更新redis数据
            redisTemplate.opsForHash().put(RedisConstant.COURSE_HASH_KEY, field, JSON.toJSONString(course));
            //将选课信息存入redis
//            redisTemplate.opsForHash().put(RedisConstant.USER_COURSE_KEY, userFieldId, JSON.toJSONString(course));
        }

        result.setMessage("选课成功");
        result.setCode("200");
        return result;
    }

    /**
     * 查询个人选课信息
     * @param userId
     * @return
     */
    @Override
    public Result<List<UserCourse>> selectUserCourseByUserId(long userId) {
        Result<List<UserCourse>> result = new Result<>();
        result.setSuccess(true);
//        List<UserCourse> userCourseList = redisTemplate.opsForHash().get(RedisConstant.USER_COURSE_KEY, userId);
        List<UserCourse> userCourseList = userCourseDAO.selectUserCourseByUserId(userId).stream()
                .map(UserCourseDO::toModel).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(userCourseList)) {
            result.setSuccess(false);
            result.setMessage("未查询到选课信息");
            return result;
        }

        int countSelectedCourse = userCourseDAO.countSelectedCourse(userId);
        result.setTotalCount(countSelectedCourse);
        result.setMessage("已查询到选课信息");
        result.setCode("200");
        result.setData(userCourseList);
        return result;
    }

    /**
     * 删除个人选课信息
     * @param userId
     * @param courseId
     * @return
     */
    @Override
    public Result<UserCourse> deleteUserCourse(long userId, long courseId) {
        Result<UserCourse> result = new Result<>();
        result.setSuccess(true);

        int deleteResult = userCourseDAO.deleteUserCourse(userId, courseId);
        if (deleteResult <= 0) {
            result.setSuccess(false);
            result.setMessage("退课失败，未找到该选课记录");
            return result;
        }

        int updateRows = courseDAO.decreasedSelectedCount(courseId);
        if (updateRows <= 0) {
            result.setSuccess(false);
            result.setMessage("退课更新课程人数失败");
            return result;
        }

        String filed = String.valueOf(courseId);
        Object courseObj = redisTemplate.opsForHash().get(RedisConstant.COURSE_HASH_KEY, filed);
        if (courseObj != null) {
            Course course = JSON.parseObject(courseObj.toString(), Course.class);
            course.setSelectedCount(course.getSelectedCount() - 1);
            course.setRemaining(course.getRemaining() + 1);
            redisTemplate.opsForHash().put(RedisConstant.COURSE_HASH_KEY, filed, JSON.toJSONString(course));
        }

        result.setMessage("退课成功");
        result.setCode("200");
        return result;
    }
}
