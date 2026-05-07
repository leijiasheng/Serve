package com.student.server.model;

import lombok.Data;

import java.util.Date;

@Data
public class snappedUser {

    long id;

    long userId;

    long productId;

    Date gmtCreated;

    Date gmtModified;
}
