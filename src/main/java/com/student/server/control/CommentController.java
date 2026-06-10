package com.student.server.control;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.student.server.annotation.RateLimit;
import com.student.server.model.Comment;
import com.student.server.model.Paging;
import com.student.server.model.Result;
import com.student.server.model.UserInfo;
import com.student.server.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/comment")
@Tag(name = "评论管理", description = "评论列表查询和发表评论接口")
public class CommentController {

    private final CommentService commentService;

    /**
     * 加载全部评论
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "获取评论列表", description = "分页查询全部评论")
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
    @Operation(summary = "发表评论", description = "用户发表评论，可回复他人评论")
    public Result<Comment> postComment(@Parameter(description = "评论内容") @RequestParam("content") String content,
                                       @Parameter(description = "父评论ID（回复时必填）") @RequestParam(required = false, defaultValue = "0") Long parentId,
                                       @Parameter(description = "被回复用户昵称（可选）") @RequestParam(name = "replyNickName", required = false)String replyNickName,
                                       HttpServletRequest request) {

        UserInfo userInfo = (UserInfo) request.getAttribute("currentUser");

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
