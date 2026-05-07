package com.student.server.dao;

import com.student.server.dataobject.OrderDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderDAO {

    int insertOrder(OrderDO orderDO);

}
