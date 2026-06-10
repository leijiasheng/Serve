package com.student.server.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录响应 VO")
public class LoginVO {

    @Schema(description = "JWT Token")
    private String token;
    @Schema(description = "用户信息")
    private UserInfo userInfo;
}
