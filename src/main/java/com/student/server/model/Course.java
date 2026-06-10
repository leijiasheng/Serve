package com.student.server.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "课程实体")
public class Course implements Serializable {

    @Schema(description = "课程ID")
    long id;

    @Schema(description = "课程编号")
    long courseNum;

    @Schema(description = "适用年级")
    List<String> grade;

    @Schema(description = "课程名称")
    String name;

    @Schema(description = "授课教师")
    String teacher;

    @Schema(description = "适用学院")
    List<String> college;

    @Schema(description = "适用专业")
    List<String> major;

    @Schema(description = "上课时间")
    String courseTime;

    @Schema(description = "上课地点")
    String courseRoom;

    @Schema(description = "学分")
    Integer credit;

    @Schema(description = "总容量")
    Integer totalCapacity;

    @Schema(description = "已选人数")
    Integer selectedCount;

    @Schema(description = "剩余名额")
    Integer remaining;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    LocalDateTime gmtCreated;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    LocalDateTime gmtModified;
}
