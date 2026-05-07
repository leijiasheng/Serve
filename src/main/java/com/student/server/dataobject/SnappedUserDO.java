package com.student.server.dataobject;

import lombok.Data;

import java.util.Date;

@Data
public class SnappedUserDO {

    long id;

    long userId;

    long productId;

    Date gmtModified;

    Date gmtCreated;
}
