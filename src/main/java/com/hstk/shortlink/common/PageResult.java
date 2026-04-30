package com.hstk.shortlink.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T>{
    private List<T> record;
    private Long total;
    private Integer page;
    private Integer pageSize;

    public PageResult(List<T> record, Long total, Integer page, Integer pageSize) {
        this.record = record;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }
}
