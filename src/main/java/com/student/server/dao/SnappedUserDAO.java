package com.student.server.dao;

import com.student.server.dataobject.SnappedUserDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SnappedUserDAO {

    int insert(SnappedUserDO snappedUserDO);

}
