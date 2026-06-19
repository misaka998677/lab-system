package com.lab.module.stock.dto;

import lombok.Data;

/**
 * 出入库提交参数。Controller 接收前端表单/JSON 后转入 service。
 */
@Data
public class StockRecordDTO {
    private Long itemId;
    private Integer qty;
    private Long reservationId;
    private Long userId;
    private String remark;
}
