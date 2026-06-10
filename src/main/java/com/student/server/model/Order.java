package com.student.server.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Schema(description = "订单实体")
public class Order {

    @Schema(description = "订单ID")
    String id;

    @Schema(description = "订单编号")
    String orderNumber;

    @Schema(description = "用户ID")
    Long userId;

    @Schema(description = "用户信息")
    User user;

    @Schema(description = "商品信息")
    Product product;

    @Schema(description = "商品ID")
    String productId;

    @Schema(description = "总价")
    Double totalPrice;

    @Schema(description = "订单状态")
    OrderStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd", timezone = "GMT+8")
    @Schema(description = "创建时间")
    Date gmtCreated;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyyMMdd", timezone = "GMT+8")
    @Schema(description = "修改时间")
    Date gmtModified;
}
