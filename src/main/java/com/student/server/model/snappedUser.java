package com.student.server.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "抢购记录")
public class snappedUser {

    @Schema(description = "ID")
    long id;

    @Schema(description = "用户ID")
    long userId;

    @Schema(description = "商品ID")
    long productId;

    @Schema(description = "创建时间")
    Date gmtCreated;

    @Schema(description = "修改时间")
    Date gmtModified;
}
