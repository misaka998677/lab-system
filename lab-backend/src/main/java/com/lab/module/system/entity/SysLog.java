package com.lab.module.system.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysLog {
    private Long   id;
    private Long   userId;
    private String username;
    private String module;
    private String action;
    private String method;
    private String params;
    private String ip;
    private Long   costMs;
    private Integer status;
    private String  errorMsg;
    private LocalDateTime createTime;
}

