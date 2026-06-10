package com.student.server.control;

import com.student.server.model.Course;
import com.student.server.model.Result;
import com.student.server.param.PageParam;
import com.student.server.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "课程管理", description = "课程查询、筛选和添加接口")
public class CourseController {

    private final CourseService courseService;

    /**
     * 查询全部课程
     * @return
     */
    @GetMapping("/get")
    @Operation(summary = "查询全部课程", description = "获取所有课程列表")
    public Result<List<Course>> findAllCourse() {
        return courseService.findAllCourse();
    }

    /**
     * 根据条件进行筛选课程
     * @param grade
     * @param college
     * @param major
     * @param param
     * @return
     */
    @GetMapping("/getByCon")
    @Operation(summary = "按条件筛选课程", description = "根据年级、学院和专业筛选课程")
    public Result<List<Course>> queryByCondition(@Parameter(description = "年级（可选）") @RequestParam(value = "grade", required = false) String grade,
                                                 @Parameter(description = "学院（可选）") @RequestParam(value = "college", required = false) String college,
                                                 @Parameter(description = "专业（可选）") @RequestParam(value = "major", required = false) String major,
                                                 @Parameter(hidden = true) PageParam param) {
        log.info("查询课程");
        return courseService.findByCondition(grade, college, major, param);
    }

    @PostMapping("/add")
    @Operation(summary = "添加课程", description = "新增一门课程")
    public Result<Course> addCourse(@RequestBody Course course) {
        log.info("添加课程");
        return courseService.addCourse(course);
    }
}
