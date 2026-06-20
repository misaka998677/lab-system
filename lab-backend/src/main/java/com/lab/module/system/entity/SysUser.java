package com.lab.module.system.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysUser {
    private Long   id;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private Integer gender;
    private String avatar;
    private Long    deptId;
    private Integer status;
    private String  remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    /** 登录失败计数（集群共享） */
    private Integer failCount;
    /** 首次失败时间（集群共享） */
    private LocalDateTime firstFailAt;
    /** 锁定截止时间（集群共享） */
    private LocalDateTime lockedUntil;

    /** 关联角色 id 列表，仅查询使用 */
    private java.util.List<Long> roleIds;
    private String deptName;
    private String roleNames;
}
