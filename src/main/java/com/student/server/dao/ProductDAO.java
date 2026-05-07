package com.student.server.dao;

import com.student.server.dataobject.ProductDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductDAO {

    int insertProduct(ProductDO productDO);

    ProductDO selectById(long id);

    int reduceStock(@Param("id") long id,@Param("count") int count);
}
