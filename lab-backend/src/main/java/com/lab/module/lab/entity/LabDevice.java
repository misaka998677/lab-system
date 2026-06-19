package com.lab.module.lab.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LabDevice {
    private Long   id;
    private String assetNo;
    private String name;
    private String category;
    private String brand;
    private String model;
    private Long   labId;
    private LocalDate purchaseDate;
    private BigDecimal price;
    private Integer status;
    private String  remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    private String labName;
    private String managerName;
}
