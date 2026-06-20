package com.lab.module.system.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 批量导入任务进度跟踪。
 * 任务完成后结果保留 10 分钟供前端查询。
 */
@Data
public class ImportTask {
    private String taskId;
    private Long userId;
    private String module;       // lab-device | stock-item
    private Integer total;       // 总行数
    private Integer processed;   // 已处理行数
    private Integer success;     // 成功行数
    private Integer fail;        // 失败行数
    private String status;       // RUNNING | DONE | ERROR
    private String errorMsg;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
}
