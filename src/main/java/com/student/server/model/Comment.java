package com.student.server.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Schema(description = "评论实体")
public class Comment {

    @Schema(description = "评论ID")
    long id;

    @Schema(description = "用户ID")
    long userId;

    @Schema(description = "评论作者")
    User author;

    @Schema(description = "评论内容")
    String content;

    @Schema(description = "父评论ID")
    long parentId;

    @Schema(description = "被回复用户昵称")
    String replyNickName;

    @Schema(description = "子评论列表")
    List<Comment> children;

    @Schema(description = "评论时间")
    LocalDateTime gmtCreated;

    @Schema(description = "修改时间")
    LocalDateTime gmtModified;

}
