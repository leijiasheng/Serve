package com.student.server.dataobject;

import com.student.server.model.User;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentDO {

    private long id;

    private long userId;

    private User author;

    private String content;

    private long parentId;

    private String replyNickName;

    private List<CommentDO> commentList;

    private LocalDateTime gmtCreated;

    private LocalDateTime gmtModified;

}
