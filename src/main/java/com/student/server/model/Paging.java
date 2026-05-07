package com.student.server.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class Paging<R> implements Serializable {

    private static final long serialVersionUID = 522660448543880825L;

    private int pageNum;

    private int pageSize = 10;

    private int totalPage;

    private long totalCount;

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
