package com.lab.module.lab.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LabDeviceRepair {
    private Long   id;
    private Long   deviceId;
    private Long   reporterId;
    private String faultDesc;
    private Long   handlerId;
    private String handleNote;
    private Integer status;
    private LocalDateTime reportTime;
    private LocalDateTime finishTime;

    private String deviceName;
    private String assetNo;
    private String reporterName;
    private String handlerName;
    private String labName;
}
