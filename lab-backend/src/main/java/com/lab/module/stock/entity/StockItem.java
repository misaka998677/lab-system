package com.lab.module.stock.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 耗材档案。
 */
@Data
public class StockItem {
    private Long   id;
    private String code;
    private String name;
    private String category;
    private String unit;
    private Integer qty;
    private Integer warnQty;
    private Long   labId;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    /** 关联展示字段：实验室名称（join 查询使用）。 */
    private String labName;
}
