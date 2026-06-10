package com.student.server.dao;

import com.student.server.dataobject.SnappedUserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SnappedUserDAO {

    int insert(SnappedUserDO snappedUserDO);

    int countByUserAndProduct(@Param("userId") long userId, @Param("productId") long productId);

}
