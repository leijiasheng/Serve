package com.student.server.param;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageParam {

    int pageNum = 1;

    int pageSize = 10;

    int totalCount;

    int totalPage;
}
