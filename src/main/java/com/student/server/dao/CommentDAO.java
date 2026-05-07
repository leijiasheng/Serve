package com.student.server.dao;

import com.student.server.model.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentDAO {

    List<Comment> findAllComments();

    int postComment(@Param("content") String content,@Param("userId") long userId,@Param("parentId") long parentId, @Param("replyNickName")String replyNickName);

}
