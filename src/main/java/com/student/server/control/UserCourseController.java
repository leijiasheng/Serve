package com.student.server.control;

import com.student.server.model.Result;
import com.student.server.model.UserCourse;
import com.student.server.model.UserInfo;
import com.student.server.service.UserCourseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/userCourse")
@Slf4j
public class UserCourseController {

    @Autowired
    private UserCourseService userCourseService;


    /**
     * 根据courseId和userId进行选课
     * @param courseId
     * @param request
     * @return
     */
    @PostMapping("/insert")
    public Result<UserCourse> insertUserCourse(@RequestParam("courseId") long courseId,
                                               HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        UserInfo user =(UserInfo) session.getAttribute("user");
        long userId = user.getId();
        Result<UserCourse> result = userCourseService.insertUserCourse(userId, courseId);
        if (result.isSuccess()) {
            log.info("选课成功");
        } else {
            log.error("选课失败");
        }
        return result;
    }

    /**
     * 根据userId查询选课信息
     * @param request
     * @return
     */
    @GetMapping("/get")
    public Result<List<UserCourse>> selectUserCourseByUseId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        UserInfo user =(UserInfo) session.getAttribute("user");
        long userId= user.getId();
        Result<List<UserCourse>> result = userCourseService.selectUserCourseByUserId(userId);
        if (result.isSuccess()) {
            log.info("获取选课信息成功");
        } else {
            log.error("获取选课信息失败，未查询到选课信息");
        }
        return result;
    }

    /**
     * 根据userId和courseId退课
     * @param courseId
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public Result<UserCourse> deleteUserCourse(@RequestParam("courseId") long courseId,
                                               HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        UserInfo user =(UserInfo) session.getAttribute("user");

        long userId = user.getId();
        Result<UserCourse> result = userCourseService.deleteUserCourse(userId, courseId);
        if (result.isSuccess()) {
            log.info("退课成功");
        } else {
            log.error("退课失败");
        }
        return result;
    }

}
