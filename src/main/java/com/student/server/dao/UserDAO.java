package com.student.server.dao;

import com.student.server.dataobject.UserDO;
import com.student.server.param.PageParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserDAO {

     int insert(UserDO userDO);

     int deleteByStudentNum(@Param("studentNum") String studentNum);

     List<UserDO> findByKey(@Param("keyWord") String keyWord,@Param("param") PageParam param);

     UserDO findByStudentNum(@Param("studentNum") String studentNum);

     int updateByStudentNum(UserDO userDO);

     int updatePwd(@Param("password") String password,@Param("studentNum") String studentNum);

     List<UserDO> findAll(PageParam param);

     int countAll();

     int countByKeyWord(@Param("keyWord") String keyWord);

     UserDO selectByUserId(long userId);

}
