package com.lab.module.system.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysRole {
    private Long   id;
    private String roleCode;
    private String roleName;
    private Integer sortNo;
    private Integer status;
    private String  remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
    private java.util.List<Long> menuIds;
}
