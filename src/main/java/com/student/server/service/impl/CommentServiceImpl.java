package com.student.server.service.impl;

import com.student.server.control.CommentController;
import com.student.server.dao.CommentDAO;
import com.student.server.model.Comment;
import com.student.server.model.Result;
import com.student.server.model.User;
import com.student.server.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    
    private final CommentDAO commentDAO;

    /**
     * 查询所有评论
     * @return
     */
    @Override
    public Result<List<Comment>> findAllComments() {
        
        Result<List<Comment>> result = new Result<>();
        
        List<Comment> comments = commentDAO.findAllComments();
        
        if (CollectionUtils.isEmpty(comments)) {
            result.setCode("500");
            result.setMessage("没有评论");
            return result;
        }

        List<Comment> parentComments = handleComments(comments);
        result.setSuccess(true);
        result.setMessage("查找评论成功");
        result.setCode("200");
        result.setData(parentComments);
        
        return result;
    }

    /**
     * 发表评论
     * @param userId
     * @param parentId
     * @param replyNickName
     * @param content
     * @return
     */
    @Override
    public Result<Comment> postComment(long userId, Long parentId, String replyNickName, String content) {

        Result<Comment> result = new Result<>();

        try {
            // 1. 字符串非空校验
            if (!StringUtils.hasText(content)) {
                result.setSuccess(false);
                result.setCode("400");
                result.setMessage("评论内容不能为空");
                return result;
            }

            // 2. 数字ID合法性校验
            if (userId <= 0) {
                result.setSuccess(false);
                result.setCode("400");
                result.setMessage("用户ID不合法");
                return result;
            }
            if (parentId < 0) {
                result.setSuccess(false);
                result.setCode("400");
                result.setMessage("父评论ID不合法");
                return result;
            }

            String body = StringEscapeUtils.escapeHtml4(content);

            // 3. 执行插入
            //先缓存到redis
//            redisTemplate.opsForList().leftPush("commentList", )
            int rows = commentDAO.postComment(body, userId, parentId, replyNickName);

            Comment comment = new Comment();
            User user = new User();
            user.setId(userId);
            comment.setReplyNickName(replyNickName);
            comment.setAuthor(user);
            comment.setContent(body);
            comment.setParentId(parentId);
            comment.setUserId(userId);

            // 4. 判断结果
            if (rows > 0) {
                result.setSuccess(true);
                result.setCode("200");
                result.setMessage("评论发布成功");
                result.setData(comment);
            } else {
                result.setSuccess(false);
                result.setCode("500");
                result.setMessage("评论发布失败，请稍后重试");
            }

        } catch (Exception e) {
            // 捕获所有异常，防止接口500
            result.setSuccess(false);
            result.setCode("500");
            result.setMessage("系统异常：" + e.getMessage());
        }

        return result;
    }

    private List<Comment> handleComments(List<Comment> comments) {

        Map<Long, Comment> commentMap = new HashMap<>();
        Comment root = new Comment();
        root.setChildren(new ArrayList<>());
        commentMap.put(0L, root);

        for (Comment comment : comments) {
            commentMap.put(comment.getId(), comment);
        }

        comments.forEach(comment -> {
            Comment parent = commentMap.get(comment.getParentId());
            if (parent != null) {
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(comment);
            } else {
                commentMap.get(0L).getChildren().add(comment);
            }
        });
        return commentMap.get(0L).getChildren();
    }
}
