package com.lab.module.stock.controller;

import com.lab.common.ExcelExportUtil;
import com.lab.common.ExcelImportUtil;
import com.lab.common.ImportResult;
import com.lab.common.PageParam;
import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.config.StatCacheInvalidator;
import com.lab.module.stock.dto.StockRecordDTO;
import com.lab.module.stock.entity.StockItem;
import com.lab.module.stock.entity.StockRecord;
import com.lab.module.stock.service.StockService;
import com.lab.security.LoginUser;
import com.lab.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 档案 CRUD + 出入库流水（支持 reservationId 联动模块3 预约单）。
 */
@RestController
@RequestMapping("/stock")
public class StockController {

    private final StockService stockService;
    private final StatCacheInvalidator cache;

    @Autowired
    public StockController(StockService stockService, StatCacheInvalidator c) {
        this.stockService = stockService;
        this.cache = c;
    }

    // ---------------- 档案 ----------------

    @GetMapping("/item/page")
    public Result<PageResult<StockItem>> itemPage(@RequestParam(defaultValue = "1") int pageNum,
                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Long labId,
                                                  @RequestParam(required = false) Integer warningOnly) {
        int[] p = PageParam.clamp(pageNum, pageSize);
        return Result.ok(stockService.itemPage(p[0], p[1], keyword, labId, warningOnly));
    }

    @GetMapping("/item/warnings")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<java.util.List<StockItem>> warnings(@RequestParam(defaultValue = "10") Integer limit) {
        int p = Math.min(limit == null ? 10 : limit, 100);
        return Result.ok(stockService.warningList(p));
    }

    @GetMapping("/item/{id}")
    public Result<StockItem> itemDetail(@PathVariable Long id) {
        return Result.ok(stockService.detail(id));
    }

    @PostMapping("/item")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<Long> itemCreate(@RequestBody StockItem item) {
        Long id = stockService.create(item);
        cache.evictStock(); cache.evictOverview();
        return Result.ok(id);
    }

    @PutMapping("/item")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<?> itemUpdate(@RequestBody StockItem item) {
        stockService.update(item);
        cache.evictStock(); cache.evictOverview();
        return Result.ok();
    }

    @DeleteMapping("/item/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<?> itemDelete(@PathVariable Long id) {
        stockService.delete(id);
        cache.evictStock(); cache.evictOverview();
        return Result.ok();
    }

    @GetMapping("/item/export")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public void itemExport(HttpServletResponse resp) throws Exception {
        List<StockItem> list = stockService.itemListAll();
        ExcelExportUtil.export("耗材档案导出.xlsx", resp,
                new String[]{"耗材编号", "名称", "实验室", "分类", "单位", "当前库存", "预警值"},
                list,
                (row, it) -> {
                    row.createCell(0).setCellValue(it.getCode() != null ? it.getCode() : "");
                    row.createCell(1).setCellValue(it.getName() != null ? it.getName() : "");
                    row.createCell(2).setCellValue(it.getLabName() != null ? it.getLabName() : "");
                    row.createCell(3).setCellValue(it.getCategory() != null ? it.getCategory() : "");
                    row.createCell(4).setCellValue(it.getUnit() != null ? it.getUnit() : "");
                    row.createCell(5).setCellValue(it.getQty() != null ? it.getQty().doubleValue() : 0.0);
                    row.createCell(6).setCellValue(it.getWarnQty() != null ? it.getWarnQty().doubleValue() : 0.0);
                });
    }

    @GetMapping("/item/template")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public void itemTemplate(HttpServletResponse resp) throws Exception {
        String[] headers = {"耗材编号", "名称", "分类", "单位", "库存", "预警阈值", "实验室"};
        String[] sample = {"HC-001", "打印纸", "办公耗材", "包", "10", "5", "请从系统中查看实验室名称或填写实验室 ID"};
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition",
            "attachment; filename=" + java.net.URLEncoder.encode("耗材档案导入模板.xlsx", java.nio.charset.StandardCharsets.UTF_8));
        try (java.io.OutputStream out = resp.getOutputStream()) {
            ExcelImportUtil.writeTemplate(out, headers, sample);
        }
    }

    @PostMapping("/item/import")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<ImportResult> itemImport(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail("请上传文件");
        }
        try {
            ImportResult result = stockService.importItems(file.getInputStream());
            cache.evictStock(); cache.evictOverview();
            return Result.ok("导入完成，共 " + result.getSuccessCount() + " 行", result);
        } catch (Exception e) {
            return Result.fail(500, e.getMessage());
        }
    }

    // ---------------- 流水 ----------------

    @GetMapping("/record/page")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<PageResult<StockRecord>> recordPage(@RequestParam(defaultValue = "1") int pageNum,
                                                      @RequestParam(defaultValue = "10") int pageSize,
                                                      @RequestParam(required = false) Long itemId,
                                                      @RequestParam(required = false) Integer type,
                                                      @RequestParam(required = false) Long reservationId) {
        int[] p = PageParam.clamp(pageNum, pageSize);
        return Result.ok(stockService.recordPage(p[0], p[1], itemId, type, reservationId));
    }

    @PostMapping("/record/in")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<Long> stockIn(@RequestBody StockRecordDTO dto) {
        fillCurrentUser(dto);
        Long id = stockService.stockIn(dto);
        cache.evictStock(); cache.evictOverview();
        return Result.ok(id);
    }

    @PostMapping("/record/out")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<Long> stockOut(@RequestBody StockRecordDTO dto) {
        fillCurrentUser(dto);
        Long id = stockService.stockOut(dto);
        cache.evictStock(); cache.evictOverview();
        return Result.ok(id);
    }

    private void fillCurrentUser(StockRecordDTO dto) {
        LoginUser cu = SecurityUtil.current();
        if (cu != null && cu.getUser() != null) {
            dto.setUserId(cu.getUser().getId());
        }
    }

    @GetMapping("/record/export")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public void recordExport(HttpServletResponse resp) throws Exception {
        List<StockRecord> list = stockService.recordListAll();
        ExcelExportUtil.export("出入库流水导出.xlsx", resp,
                new String[]{"操作类型", "耗材名称", "操作人", "数量", "时间", "关联预约单"},
                list,
                (row, rc) -> {
                    row.createCell(0).setCellValue(typeText(rc.getType()));
                    row.createCell(1).setCellValue(rc.getItemName() != null ? rc.getItemName() : "");
                    row.createCell(2).setCellValue(rc.getOperatorName() != null ? rc.getOperatorName() : "");
                    row.createCell(3).setCellValue(rc.getQty() != null ? rc.getQty().doubleValue() : 0.0);
                    row.createCell(4).setCellValue(rc.getCreateTime() != null ? rc.getCreateTime().toString() : "");
                    row.createCell(5).setCellValue(rc.getReservationNo() != null ? rc.getReservationNo() : "");
                });
    }

    private String typeText(Integer type) {
        if (type == null) return "";
        switch (type) {
            case 1: return "入库";
            case 2: return "出库";
            case 3: return "盘点";
            default: return String.valueOf(type);
        }
    }
}
