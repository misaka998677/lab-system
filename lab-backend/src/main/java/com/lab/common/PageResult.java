package com.lab.common;

import com.github.pagehelper.PageInfo;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private List<T> records;

    public static <T> PageResult<T> of(PageInfo<T> info) {
        PageResult<T> r = new PageResult<>();
        r.total = info.getTotal();
        r.records = info.getList();
        return r;
    }

    public static <T> PageResult<T> empty() {
        PageResult<T> r = new PageResult<>();
        r.total = 0;
        r.records = Collections.emptyList();
        return r;
    }
}
