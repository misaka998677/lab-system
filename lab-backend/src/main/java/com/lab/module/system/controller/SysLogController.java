package com.lab.module.system.controller;

import com.lab.common.ExcelExportUtil;
import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.module.system.entity.SysLog;
import com.lab.module.system.service.SysLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/system/log")
public class SysLogController {
    private final SysLogService logService;
    public SysLogController(SysLogService s) { this.logService = s; }

    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<SysLog>> page(@RequestParam(defaultValue = "1")  int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           @RequestParam(required = false) String username,
                                           @RequestParam(required = false) String module,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return Result.ok(logService.page(pageNum, pageSize, username, module, status, start, end));
    }

    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> clear() { logService.clear(); return Result.ok(); }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void export(HttpServletResponse resp) throws Exception {
        List<SysLog> logs = logService.all();
        ExcelExportUtil.export("操作日志.xlsx", resp,
                new String[]{"ID", "操作时间", "操作人", "模块", "操作类型", "请求方法", "IP地址", "耗时(ms)", "状态", "详情"},
                logs,
                (row, log) -> {
                    row.createCell(0).setCellValue(log.getId() != null ? log.getId() : 0);
                    row.createCell(1).setCellValue(log.getCreateTime() != null ? log.getCreateTime().toString() : "");
                    row.createCell(2).setCellValue(log.getUsername() != null ? log.getUsername() : "");
                    row.createCell(3).setCellValue(log.getModule() != null ? log.getModule() : "");
                    row.createCell(4).setCellValue(log.getAction() != null ? log.getAction() : "");
                    row.createCell(5).setCellValue(log.getMethod() != null ? log.getMethod() : "");
                    row.createCell(6).setCellValue(log.getIp() != null ? log.getIp() : "");
                    row.createCell(7).setCellValue(log.getCostMs() != null ? log.getCostMs().doubleValue() : 0);
                    row.createCell(8).setCellValue(log.getStatus() != null ? log.getStatus().doubleValue() : 0);
                    row.createCell(9).setCellValue(log.getErrorMsg() != null ? log.getErrorMsg() : "");
                });
    }
}
