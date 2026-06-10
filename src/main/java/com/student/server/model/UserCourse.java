package com.student.server.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户选课关联实体")
public class UserCourse {

    @Schema(description = "ID")
    private long id;

    @Schema(description = "用户ID")
    private long userId;

    @Schema(description = "课程ID")
    private long courseId;

    @Schema(description = "用户信息")
    private User user;

    @Schema(description = "课程信息")
    private Course course;



}
