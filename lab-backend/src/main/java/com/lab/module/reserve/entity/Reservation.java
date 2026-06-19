package com.lab.module.reserve.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Reservation {
    private Long   id;
    private String reserveNo;
    private Long   userId;
    private Long   teacherId;
    private Long   labId;
    private String deviceIds;
    private String purpose;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime endTime;
    private Integer status;
    private Long   auditUserId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime auditTime;
    private String  auditNote;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime checkInTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime checkOutTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime updateTime;
    private Integer deleted;

    private String userName;
    private String teacherName;
    private String labName;
    private String auditUserName;
}
