package com.lab.module.stat.controller;

import com.lab.common.ExcelExportUtil;
import com.lab.common.Result;
import com.lab.module.stat.service.StatService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stat")
public class StatController {

    private final StatService statService;

    public StatController(StatService statService) {
        this.statService = statService;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(statService.overview());
    }

    @GetMapping("/lab-usage")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<Map<String, Object>> labUsage() {
        return Result.ok(statService.labUsage());
    }

    @GetMapping("/device-fault")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<Map<String, Object>> deviceFault() {
        return Result.ok(statService.deviceFault());
    }

    @GetMapping("/stock-warning")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<Map<String, Object>> stockWarning() {
        return Result.ok(statService.stockWarning());
    }

    @GetMapping("/overview/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void overviewExport(HttpServletResponse resp) throws Exception {
        Map<String, Object> data = statService.overview();
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("工作台总览");
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont(); headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            Cell headerCell = headerRow.createCell(0);
            headerCell.setCellValue("统计项");
            headerCell.setCellStyle(headerStyle);
            headerRow.createCell(1).setCellValue("数值");
            headerRow.getCell(1).setCellStyle(headerStyle);

            Object[][] stats = {
                {"今日预约数",         ((Number) data.getOrDefault("todayReservations", 0)).doubleValue()},
                {"待审核预约数",       ((Number) data.getOrDefault("pendingReservations", 0)).doubleValue()},
                {"实验室总数",         ((Number) data.getOrDefault("labCount", 0)).doubleValue()},
                {"设备总数",           ((Number) data.getOrDefault("deviceCount", 0)).doubleValue()},
                {"待处理维修单数",     ((Number) data.getOrDefault("pendingRepairs", 0)).doubleValue()},
                {"库存预警数",         ((Number) data.getOrDefault("stockWarnings", 0)).doubleValue()},
            };
            for (int i = 0; i < stats.length; i++) {
                Row r = sheet.createRow(i + 1);
                r.createCell(0).setCellValue((String) stats[i][0]);
                r.createCell(1).setCellValue(((Number) stats[i][1]).doubleValue());
            }
            sheet.setColumnWidth(0, 4000);
            sheet.setColumnWidth(1, 2500);
            ExcelExportUtil.writeWorkbook("工作台总览.xlsx", wb, resp);
        }
    }

    @GetMapping("/usage/export")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public void usageExport(HttpServletResponse resp) throws Exception {
        Map<String, Object> usageData = statService.labUsage();
        List<Map<String, Object>> ranking = (List<Map<String, Object>>) usageData.getOrDefault("ranking", List.of());
        ExcelExportUtil.export("使用率分析.xlsx", resp,
                new String[]{"实验室名称", "预约次数", "总使用时长(h)"},
                ranking,
                (row, rowMap) -> {
                    row.createCell(0).setCellValue(String.valueOf(rowMap.getOrDefault("labName", "")));
                    row.createCell(1).setCellValue(((Number) rowMap.getOrDefault("reserveCount", 0)).doubleValue());
                    row.createCell(2).setCellValue(((Number) rowMap.getOrDefault("useHours", 0)).doubleValue());
                });
    }

    @GetMapping("/device-fault/export")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public void faultExport(HttpServletResponse resp) throws Exception {
        Map<String, Object> data = statService.deviceFault();
        List<Map<String, Object>> deviceStatus = (List<Map<String, Object>>) data.getOrDefault("deviceStatus", List.of());
        List<Map<String, Object>> repairStatus = (List<Map<String, Object>>) data.getOrDefault("repairStatus", List.of());
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont(); headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Sheet s1 = wb.createSheet("设备状态分布");
            Row h1 = s1.createRow(0);
            h1.createCell(0).setCellValue("状态"); h1.getCell(0).setCellStyle(headerStyle);
            h1.createCell(1).setCellValue("数量"); h1.getCell(1).setCellStyle(headerStyle);
            for (int i = 0; i < deviceStatus.size(); i++) {
                Map<String, Object> row = deviceStatus.get(i);
                Row r = s1.createRow(i + 1);
                r.createCell(0).setCellValue(String.valueOf(row.getOrDefault("status", "")));
                r.createCell(1).setCellValue(((Number)row.getOrDefault("count", 0)).doubleValue());
            }

            Sheet s2 = wb.createSheet("维修状态分布");
            Row h2 = s2.createRow(0);
            h2.createCell(0).setCellValue("状态"); h2.getCell(0).setCellStyle(headerStyle);
            h2.createCell(1).setCellValue("数量"); h2.getCell(1).setCellStyle(headerStyle);
            for (int i = 0; i < repairStatus.size(); i++) {
                Map<String, Object> row = repairStatus.get(i);
                Row r = s2.createRow(i + 1);
                r.createCell(0).setCellValue(String.valueOf(row.getOrDefault("status", "")));
                r.createCell(1).setCellValue(((Number)row.getOrDefault("count", 0)).doubleValue());
            }
            ExcelExportUtil.writeWorkbook("设备故障分析.xlsx", wb, resp);
        }
    }

    @GetMapping("/stock/export")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public void stockExport(HttpServletResponse resp) throws Exception {
        Map<String, Object> data = statService.stockWarning();
        List<?> warnings = (List<?>) data.getOrDefault("warningList", List.of());
        java.beans.PropertyDescriptor[] props = warnings.isEmpty() ? new java.beans.PropertyDescriptor[0]
                : java.beans.Introspector.getBeanInfo(warnings.get(0).getClass(), Object.class).getPropertyDescriptors();
        java.util.Map<String, java.beans.PropertyDescriptor> pdMap = new java.util.HashMap<>();
        for (java.beans.PropertyDescriptor pd : props) pdMap.put(pd.getName(), pd);
        String[] fields = {"name", "category", "qty", "warnQty", "labName"};
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("库存预警");
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont(); headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("耗材名称");
            header.createCell(1).setCellValue("分类");
            header.createCell(2).setCellValue("当前库存");
            header.createCell(3).setCellValue("预警阈值");
            header.createCell(4).setCellValue("所属实验室");
            for (int i = 0; i < 5; i++) header.getCell(i).setCellStyle(headerStyle);
            for (int i = 0; i < warnings.size(); i++) {
                Object item = warnings.get(i);
                Row r = sheet.createRow(i + 1);
                for (int j = 0; j < fields.length; j++) {
                    java.beans.PropertyDescriptor pd = pdMap.get(fields[j]);
                    Object v = pd == null ? null : pd.getReadMethod().invoke(item);
                    r.createCell(j).setCellValue(v == null ? "" : v.toString());
                }
            }
            for (int i = 0; i < 5; i++) sheet.setColumnWidth(i, 3500);
            ExcelExportUtil.writeWorkbook("库存预警.xlsx", wb, resp);
        }
    }
}
