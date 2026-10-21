package com.lab.module.lab.controller;

import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.module.lab.entity.LabDevice;
import com.lab.module.lab.service.LabDeviceService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/lab/device")
public class LabDeviceController {
    private final LabDeviceService deviceService;
    public LabDeviceController(LabDeviceService s) { this.deviceService = s; }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('lab:device:list')")
    public Result<PageResult<LabDevice>> page(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Long labId,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(required = false) Integer status) {
        return Result.ok(deviceService.page(pageNum, pageSize, keyword, labId, category, status));
    }

    /** 某实验室的设备列表（所有登录用户可查看） */
    @GetMapping("/by-lab/{labId}")
    public Result<List<LabDevice>> byLab(@PathVariable Long labId) { return Result.ok(deviceService.findByLab(labId)); }

    /** 设备详情（所有登录用户可查看） */
    @GetMapping("/{id}")
    public Result<LabDevice> detail(@PathVariable Long id) { return Result.ok(deviceService.detail(id)); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<Long> create(@RequestBody LabDevice d) { return Result.ok(deviceService.create(d)); }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<?> update(@RequestBody LabDevice d) { deviceService.update(d); return Result.ok(); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<?> delete(@PathVariable Long id) { deviceService.delete(id); return Result.ok(); }

    @PutMapping("/{id}/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<?> status(@PathVariable Long id, @PathVariable Integer status) {
        deviceService.status(id, status); return Result.ok();
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('lab:device:list')")
    public void export(HttpServletResponse resp) throws IOException {
        List<LabDevice> devices = deviceService.listAll();
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("设备台账");
        CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        String[] headers = {"设备名称", "实验室", "分类", "型号", "状态", "购买日期", "负责人"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        String[] statusText = {"在用", "维修", "报废"};
        int rowIdx = 1;
        for (LabDevice d : devices) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(d.getName());
            row.createCell(1).setCellValue(d.getLabName());
            row.createCell(2).setCellValue(d.getCategory());
            row.createCell(3).setCellValue(d.getModel());
            row.createCell(4).setCellValue(d.getStatus() != null ? statusText[d.getStatus() - 1] : "");
            row.createCell(5).setCellValue(d.getPurchaseDate() != null ? d.getPurchaseDate().toString() : "");
            row.createCell(6).setCellValue(d.getManagerName());
        }

        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
        String filename = URLEncoder.encode("设备台账", StandardCharsets.UTF_8) + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename=" + filename);
        wb.write(resp.getOutputStream());
        wb.close();
    }
}
