package com.lab.module.stock.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 出入库流水。
 *
 * <p>type: 1=入库, 2=出库, 3=盘点。</p>
 */
@Data
public class StockRecord {
    private Long   id;
    private Long   itemId;
    /** 1=入库 2=出库 3=盘点 */
    private Integer type;
    private Integer qty;
    /** 关联预约单 id，用于模块联动。 */
    private Long   reservationId;
    /** 经办人 id。 */
    private Long   userId;
    private String remark;
    private LocalDateTime createTime;

    /** 展示字段：耗材名称。 */
    private String itemName;
    /** 展示字段：耗材编码。 */
    private String itemCode;
    /** 展示字段：耗材单位。 */
    private String unit;
    /** 展示字段：经办人姓名。 */
    private String operatorName;
    /** 展示字段：预约单号。 */
    private String reservationNo;
}
