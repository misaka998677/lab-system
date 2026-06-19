package com.lab.module.system.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysDept {
    private Long   id;
    private Long   parentId;
    private String name;
    private String leader;
    private String phone;
    private String email;
    private Integer sortNo;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
