package com.student.server.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@Schema(description = "统一响应结果")
public class Result<T> implements Serializable {

    @Schema(description = "响应消息")
    String message;

    @Schema(description = "响应码")
    String code;

    @Schema(description = "是否成功")
    boolean isSuccess;

    @Schema(description = "响应数据")
    T data;

    @Schema(description = "当前页码")
    int pageNum = 1;

    @Schema(description = "每页条数")
    int pageSize = 10;

    @Schema(description = "总页数")
    int totalPage;

    @Schema(description = "总条数")
    int totalCount;

}
