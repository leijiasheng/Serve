package com.student.server.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "分页结果")
public class Paging<R> implements Serializable {

    private static final long serialVersionUID = 522660448543880825L;

    @Schema(description = "当前页码")
    private int pageNum;

    @Schema(description = "每页条数")
    private int pageSize = 10;

    @Schema(description = "总页数")
    private int totalPage;

    @Schema(description = "总条数")
    private long totalCount;

    @Schema(description = "数据列表")
    private List<R> data;

    public Paging() {

    }

    public Paging(int pageNum, int pageSize, int totalPage, long totalCount, List<R> data) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.totalPage = totalPage;
        this.totalCount = totalCount;
        this.data = data;
    }
}
