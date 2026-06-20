package com.lab.module.lab.controller;

import com.lab.common.ExcelExportUtil;
import com.lab.common.ExcelImportUtil;
import com.lab.common.ImportResult;
import com.lab.common.PageParam;
import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.config.StatCacheInvalidator;
import com.lab.module.lab.entity.LabDevice;
import com.lab.module.lab.service.LabDeviceService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/lab/device")
public class LabDeviceController {
    private final LabDeviceService deviceService;
    private final StatCacheInvalidator cache;

    @Autowired
    public LabDeviceController(LabDeviceService s, StatCacheInvalidator c) { this.deviceService = s; this.cache = c; }

    @GetMapping("/page")
    public Result<PageResult<LabDevice>> page(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Long labId,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(required = false) Integer status) {
        int[] p = PageParam.clamp(pageNum, pageSize);
        return Result.ok(deviceService.page(p[0], p[1], keyword, labId, category, status));
    }

    /** 某实验室的设备列表（所有登录用户可查看） */
    @GetMapping("/by-lab/{labId}")
    public Result<List<LabDevice>> byLab(@PathVariable Long labId) { return Result.ok(deviceService.findByLab(labId)); }

    /** 设备详情（所有登录用户可查看） */
    @GetMapping("/{id}")
    public Result<LabDevice> detail(@PathVariable Long id) { return Result.ok(deviceService.detail(id)); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<Long> create(@RequestBody LabDevice d) {
        Long id = deviceService.create(d);
        cache.evictOverview(); cache.evictFault();
        return Result.ok(id);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<?> update(@RequestBody LabDevice d) {
        deviceService.update(d);
        cache.evictOverview(); cache.evictFault();
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<?> delete(@PathVariable Long id) {
        deviceService.delete(id);
        cache.evictOverview(); cache.evictFault();
        return Result.ok();
    }

    @PutMapping("/{id}/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<?> status(@PathVariable Long id, @PathVariable Integer status) {
        deviceService.status(id, status);
        cache.evictOverview(); cache.evictFault();
        return Result.ok();
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public void export(HttpServletResponse resp) throws Exception {
        List<LabDevice> devices = deviceService.listAll();
        final String[] statusText = {"在用", "维修", "报废"};
        ExcelExportUtil.export("设备台账.xlsx", resp,
                new String[]{"设备名称", "实验室", "分类", "型号", "状态", "购买日期", "负责人"},
                devices,
                (row, d) -> {
                    row.createCell(0).setCellValue(d.getName());
                    row.createCell(1).setCellValue(d.getLabName());
                    row.createCell(2).setCellValue(d.getCategory());
                    row.createCell(3).setCellValue(d.getModel());
                    row.createCell(4).setCellValue(d.getStatus() != null ? statusText[d.getStatus() - 1] : "");
                    row.createCell(5).setCellValue(d.getPurchaseDate() != null ? d.getPurchaseDate().toString() : "");
                    row.createCell(6).setCellValue(d.getManagerName());
                });
    }

    @GetMapping("/template")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public void template(HttpServletResponse resp) throws Exception {
        String[] headers = {"资产编号", "名称", "分类", "品牌", "型号", "实验室", "状态"};
        String[] sample = {"AS-001", "投影仪", "多媒体设备", "EPSON", "EB-X05", "请填实验室名称或 ID", "在用"};
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition",
            "attachment; filename=" + java.net.URLEncoder.encode("设备台账导入模板.xlsx", java.nio.charset.StandardCharsets.UTF_8));
        try (java.io.OutputStream out = resp.getOutputStream()) {
            ExcelImportUtil.writeTemplate(out, headers, sample);
        }
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<ImportResult> importDevices(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) return Result.fail("请上传文件");
        try {
            ImportResult result = deviceService.importDevices(file.getInputStream());
            cache.evictOverview(); cache.evictFault();
            return Result.ok("导入完成，共 " + result.getSuccessCount() + " 行", result);
        } catch (Exception e) {
            return Result.fail(500, e.getMessage());
        }
    }

    /**
     * 设备导入（异步版，支持进度查询）。
     * 返回 taskId，前端轮询 GET /import-task/{taskId} 获取进度。
     */
    @PostMapping("/import-async")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<String> importDevicesAsync(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) return Result.fail("请上传文件");
        try {
            String taskId = deviceService.importDevicesAsync(file.getInputStream());
            return Result.ok("导入已开始", taskId);
        } catch (Exception e) {
            return Result.fail(500, e.getMessage());
        }
    }
}
