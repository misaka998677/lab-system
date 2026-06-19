package com.lab.module.lab.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LabRoom {
    private Long   id;
    private String code;
    private String name;
    private String building;
    private String roomNo;
    private Integer capacity;
    private Long   deptId;
    private Long   managerId;
    private Integer status;
    private String  qrCode;
    private String  remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    private String deptName;
    private String managerName;
}
