package com.student.server.control;

import com.student.server.email.EmailClient;
import com.student.server.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "用户反馈", description = "用户建议反馈接口")
public class FeedbackController {

    private final EmailClient emailClient;

    @PostMapping("/api/feedback")
    @Operation(summary = "提交反馈建议")
    public Result<String> submitFeedback(@RequestParam("suggestion") String suggestion,
                                          @RequestParam(value = "contact", required = false, defaultValue = "") String contact) {
        Result<String> result = new Result<>();


        //建议kafka异步发送消息

        try {
            emailClient.sendSuggestionEmail(suggestion, contact);
            result.setSuccess(true);
            result.setMessage("感谢你的建议！");
            result.setCode("200");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage(e.getMessage());
        }
        return result;
    }
}
