package com.student.server.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class Comment {

    long id;

    long userId;

    User author;

    String content;

    long parentId;

    String replyNickName;

    //子评论
    List<Comment> children;

    LocalDateTime gmtCreated;

    LocalDateTime gmtModified;

}
