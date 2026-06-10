package com.student.server.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "用户实体")
public class User implements Serializable {

    @Schema(description = "用户ID")
    long id;

    @Schema(description = "用户名")
    String userName;

    @Schema(description = "昵称")
    String nickName;

    @JsonSerialize(using = NullSerializer.class)
    @Schema(description = "密码（不返回）")
    String password;

    @Schema(description = "邮箱")
    String email;

    @Schema(description = "学号")
    String studentNum;

    @Schema(description = "个人签名")
    String personSign;

    @Schema(description = "头像URL")
    String avatar;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    LocalDateTime gmtCreated;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    LocalDateTime gmtModified;

}
