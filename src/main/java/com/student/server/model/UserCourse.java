package com.student.server.model;

import lombok.Data;

@Data
public class UserCourse {

    private long id;

    private long userId;

    private long courseId;

    private User user;

    private Course course;



}
