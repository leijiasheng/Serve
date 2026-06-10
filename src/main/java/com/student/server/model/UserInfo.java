package com.student.server.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "用户信息（脱敏，不含密码）")
public class UserInfo implements Serializable {

    @Schema(description = "用户ID")
    long id;

    @Schema(description = "用户名")
    String userName;

    @Schema(description = "学号")
    String studentNum;

    @Schema(description = "昵称")
    String nickName;

    @Schema(description = "邮箱")
    String email;

    @Schema(description = "个人签名")
    String personSign;

    @Schema(description = "头像URL")
    String avatar;

}
