package com.student.server.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Course implements Serializable {
    long id;

    long courseNum;

    List<String> grade;

    String name;

    String teacher;

    List<String> college;

    List<String> major;

    String courseTime;

    String courseRoom;

    Integer credit;

    Integer totalCapacity;

    Integer selectedCount;

    Integer remaining;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime gmtCreated;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime gmtModified;
}
