package com.student.server.control;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.student.server.model.Comment;
import com.student.server.model.Paging;
import com.student.server.model.Result;
import com.student.server.model.UserInfo;
import com.student.server.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;


    /**
     * 加载全部评论
     * @return
     */
    @GetMapping("/list")
    public Result<List<Comment>> toComment() {
        Result<List<Comment>> result = commentService.findAllComments();
        Page<Comment> page = PageHelper.startPage(1, 10).doSelectPage(() -> commentService.findAllComments());
        Paging paging = new Paging(page.getPageNum(), page.getPageSize(), page.getPages(), page.getTotal(), page.getResult());

        log.info("进入评论列表，评论总条数：{}", paging.getTotalCount());
        result.setTotalCount((int) paging.getTotalCount());
        result.setPageNum(paging.getPageNum());
        result.setTotalPage(paging.getTotalPage());

        return result;
    }

    /**
     * 发表评论
     * @param content
     * @param parentId
     * @param replyNickName
     * @param request
     * @return
     */
    @PostMapping("/publish")
    public Result<Comment> postComment(@RequestParam("content") String content, @RequestParam(required = false, defaultValue = "0") Long parentId, @RequestParam(name = "replyNickName", required = false)String replyNickName, HttpServletRequest request) {
        HttpSession session = request.getSession();

        UserInfo userInfo = (UserInfo) session.getAttribute("user");

        long userId = userInfo.getId();

        Result<Comment> result = commentService.postComment(userId, parentId, replyNickName, content);

        if (result.isSuccess()) {
            log.info("评论发布成功");
            return result;
        } else {
            log.warn("评论发表失败");
            return new Result<>();
        }
    }
}
