package com.lab.module.lab.controller;

import com.lab.common.ExcelExportUtil;
import com.lab.common.PageParam;
import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.config.StatCacheInvalidator;
import com.lab.module.lab.entity.LabDeviceRepair;
import com.lab.module.lab.service.LabDeviceRepairService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/lab/repair")
public class LabDeviceRepairController {
    private final LabDeviceRepairService repairService;
    private final StatCacheInvalidator cache;

    @Autowired
    public LabDeviceRepairController(LabDeviceRepairService s, StatCacheInvalidator c) { this.repairService = s; this.cache = c; }

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<PageResult<LabDeviceRepair>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) Long deviceId) {
        int[] p = PageParam.clamp(pageNum, pageSize);
        return Result.ok(repairService.page(p[0], p[1], keyword, status, deviceId));
    }

    @GetMapping("/mine")
    public Result<PageResult<LabDeviceRepair>> mine(@RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Integer status) {
        int[] p = PageParam.clamp(pageNum, pageSize);
        return Result.ok(repairService.mine(p[0], p[1], keyword, status));
    }

    @PostMapping("/report")
    public Result<Long> report(@RequestBody LabDeviceRepair r) {
        Long id = repairService.report(r);
        cache.evictOverview(); cache.evictFault();
        return Result.ok(id);
    }

    @PutMapping("/{id}/handle")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<?> handle(@PathVariable Long id,
                            @RequestParam(required = false) Long handlerId,
                            @RequestParam(required = false) String note,
                            @RequestParam Integer status) {
        repairService.handle(id, handlerId, note, status);
        cache.evictOverview(); cache.evictFault();
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<?> delete(@PathVariable Long id) {
        repairService.delete(id);
        cache.evictOverview(); cache.evictFault();
        return Result.ok();
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public void export(HttpServletResponse resp) throws Exception {
        List<LabDeviceRepair> list = repairService.listAll();
        ExcelExportUtil.export("维修单导出.xlsx", resp,
                new String[]{"报修设备", "实验室", "报修人", "描述", "状态", "提交时间", "处理人", "处理时间"},
                list,
                (row, r) -> {
                    row.createCell(0).setCellValue(r.getDeviceName() != null ? r.getDeviceName() : "");
                    row.createCell(1).setCellValue(r.getLabName() != null ? r.getLabName() : "");
                    row.createCell(2).setCellValue(r.getReporterName() != null ? r.getReporterName() : "");
                    row.createCell(3).setCellValue(r.getFaultDesc() != null ? r.getFaultDesc() : "");
                    row.createCell(4).setCellValue(statusText(r.getStatus()));
                    row.createCell(5).setCellValue(r.getReportTime() != null ? r.getReportTime().toString() : "");
                    row.createCell(6).setCellValue(r.getHandlerName() != null ? r.getHandlerName() : "");
                    row.createCell(7).setCellValue(r.getFinishTime() != null ? r.getFinishTime().toString() : "");
                });
    }

    private String statusText(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待处理";
            case 1: return "处理中";
            case 2: return "已完成";
            default: return String.valueOf(status);
        }
    }
}
