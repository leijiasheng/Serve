package com.student.server.control;

import com.student.server.annotation.RateLimit;
import com.student.server.model.Result;
import com.student.server.model.UserCourse;
import com.student.server.model.UserInfo;
import com.student.server.service.UserCourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/userCourse")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "选课管理", description = "学生选课、退课和查询选课信息接口")
public class UserCourseController {

    private final UserCourseService userCourseService;


    /**
     * 根据courseId和userId进行选课
     * @param courseId
     * @param request
     * @return
     */
    @PostMapping("/insert")
    @Operation(summary = "选课", description = "根据课程ID进行选课")
    public Result<UserCourse> insertUserCourse(@Parameter(description = "课程ID") @RequestParam("courseId") long courseId,
                                               HttpServletRequest request) {
        UserInfo user =(UserInfo) request.getAttribute("currentUser");
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
    @Operation(summary = "查询选课信息", description = "根据当前登录用户查询选课列表")
    public Result<List<UserCourse>> selectUserCourseByUseId(HttpServletRequest request) {
        UserInfo user =(UserInfo) request.getAttribute("currentUser");
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
    @Operation(summary = "退课", description = "根据课程ID退课")
    public Result<UserCourse> deleteUserCourse(@Parameter(description = "课程ID") @RequestParam("courseId") long courseId,
                                               HttpServletRequest request) {
        UserInfo user =(UserInfo) request.getAttribute("currentUser");

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
