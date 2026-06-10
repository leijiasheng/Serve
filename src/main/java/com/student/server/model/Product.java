package com.student.server.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "商品实体（抢购商品）")
public class Product implements Serializable {

    @Schema(description = "商品ID")
    long id;

    @Schema(description = "商品名称")
    String name;

    @Schema(description = "商品价格")
    Double price;

    @Schema(description = "库存数量")
    int stock;

    @Schema(description = "商品图片URL")
    String url;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    LocalDateTime gmtCreated;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "修改时间")
    LocalDateTime gmtModified;

}
