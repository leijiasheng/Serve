package com.student.server.service;

import com.student.server.model.Comment;
import com.student.server.model.Result;

import java.util.List;

public interface CommentService {

    public Result<List<Comment>> findAllComments();

    public Result<Comment> postComment(long userId, Long parentId, String replyNickName, String content);
}
