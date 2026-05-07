package com.student.server.control;

import com.student.server.model.Course;
import com.student.server.model.Result;
import com.student.server.param.PageParam;
import com.student.server.service.CourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/course")
@Slf4j
public class CourseController {

    @Autowired
    private CourseService courseService;

    /**
     * 查询全部课程
     * @return
     */
    @GetMapping("/get")
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
    public Result<List<Course>> queryByCondition(@RequestParam(value = "grade", required = false) String grade,@RequestParam(value = "college", required = false) String college,@RequestParam(value = "major", required = false) String major, PageParam param) {
        log.info("查询课程");
        return courseService.findByCondition(grade, college, major, param);
    }

    @PostMapping("/add")
    public Result<Course> addCourse(@RequestBody Course course) {
        log.info("添加课程");
        return courseService.addCourse(course);
    }
}
