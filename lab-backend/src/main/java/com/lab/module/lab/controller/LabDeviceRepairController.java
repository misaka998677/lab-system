package com.lab.module.lab.controller;

import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.module.lab.entity.LabDeviceRepair;
import com.lab.module.lab.service.LabDeviceRepairService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/lab/repair")
public class LabDeviceRepairController {
    private final LabDeviceRepairService repairService;
    public LabDeviceRepairController(LabDeviceRepairService s) { this.repairService = s; }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('lab:repair:list')")
    public Result<PageResult<LabDeviceRepair>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) Long deviceId) {
        return Result.ok(repairService.page(pageNum, pageSize, keyword, status, deviceId));
    }

    @PostMapping("/report")
    public Result<Long> report(@RequestBody LabDeviceRepair r) { return Result.ok(repairService.report(r)); }

    @PutMapping("/{id}/handle")
    @PreAuthorize("hasAuthority('lab:repair:list')")
    public Result<?> handle(@PathVariable Long id,
                            @RequestParam(required = false) Long handlerId,
                            @RequestParam(required = false) String note,
                            @RequestParam Integer status) {
        repairService.handle(id, handlerId, note, status); return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('lab:repair:list')")
    public Result<?> delete(@PathVariable Long id) { repairService.delete(id); return Result.ok(); }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('lab:repair:list')")
    public void export(HttpServletResponse resp) throws Exception {
        List<LabDeviceRepair> list = repairService.listAll();

        Workbook wb = new XSSFWorkbook();
        Sheet s = wb.createSheet("维修单");
        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont(); headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        Row h = s.createRow(0);
        String[] headers = {"报修设备", "实验室", "报修人", "描述", "状态", "提交时间", "处理人", "处理时间"};
        for (int i = 0; i < headers.length; i++) {
            h.createCell(i).setCellValue(headers[i]);
            h.getCell(i).setCellStyle(headerStyle);
        }

        int r = 1;
        for (LabDeviceRepair row : list) {
            Row rowObj = s.createRow(r++);
            rowObj.createCell(0).setCellValue(row.getDeviceName() != null ? row.getDeviceName() : "");
            rowObj.createCell(1).setCellValue(row.getLabName() != null ? row.getLabName() : "");
            rowObj.createCell(2).setCellValue(row.getReporterName() != null ? row.getReporterName() : "");
            rowObj.createCell(3).setCellValue(row.getFaultDesc() != null ? row.getFaultDesc() : "");
            rowObj.createCell(4).setCellValue(statusText(row.getStatus()));
            rowObj.createCell(5).setCellValue(row.getReportTime() != null ? row.getReportTime().toString() : "");
            rowObj.createCell(6).setCellValue(row.getHandlerName() != null ? row.getHandlerName() : "");
            rowObj.createCell(7).setCellValue(row.getFinishTime() != null ? row.getFinishTime().toString() : "");
        }

        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition",
            "attachment; filename=" + URLEncoder.encode("维修单导出.xlsx", StandardCharsets.UTF_8));
        try (OutputStream out = resp.getOutputStream()) { wb.write(out); }
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
