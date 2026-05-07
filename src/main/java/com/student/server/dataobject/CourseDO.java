package com.student.server.dataobject;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.student.server.model.Course;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CourseDO {
    private long id;

    private long courseNum;

    private List<String> grade;

    private String name;

    private   String teacher;

    private List<String> college;

    private List<String> major;

    private String courseTime;

    private String courseRoom;

    private Integer credit;

    private Integer totalCapacity;

    private Integer selectedCount;

    private Integer remaining;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreated;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtModified;

    public Course toModel() {
        Course course = new Course();
        BeanUtils.copyProperties(this, course);
        return course;
    }
}
