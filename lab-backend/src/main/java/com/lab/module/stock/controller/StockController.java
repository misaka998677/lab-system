package com.lab.module.stock.controller;

import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.module.stock.dto.StockRecordDTO;
import com.lab.module.stock.entity.StockItem;
import com.lab.module.stock.entity.StockRecord;
import com.lab.module.stock.service.StockService;
import com.lab.security.LoginUser;
import com.lab.security.SecurityUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 *
 * <p>档案 CRUD + 出入库流水（支持 reservationId 联动模块3 预约单）。</p>
 */
@RestController
@RequestMapping("/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    // ---------------- 档案 ----------------

    @GetMapping("/item/page")
    @PreAuthorize("hasAuthority('stock:item:list')")
    public Result<PageResult<StockItem>> itemPage(@RequestParam(defaultValue = "1") int pageNum,
                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Long labId,
                                                  @RequestParam(required = false) Integer warningOnly) {
        return Result.ok(stockService.itemPage(pageNum, pageSize, keyword, labId, warningOnly));
    }

    @GetMapping("/item/warnings")
    @PreAuthorize("hasAuthority('stock:item:list')")
    public Result<java.util.List<StockItem>> warnings(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(stockService.warningList(limit));
    }

    @GetMapping("/item/{id}")
    @PreAuthorize("hasAuthority('stock:item:list')")
    public Result<StockItem> itemDetail(@PathVariable Long id) {
        return Result.ok(stockService.detail(id));
    }

    @PostMapping("/item")
    @PreAuthorize("hasAuthority('stock:item:list')")
    public Result<Long> itemCreate(@RequestBody StockItem item) {
        return Result.ok(stockService.create(item));
    }

    @PutMapping("/item")
    @PreAuthorize("hasAuthority('stock:item:list')")
    public Result<?> itemUpdate(@RequestBody StockItem item) {
        stockService.update(item);
        return Result.ok();
    }

    @DeleteMapping("/item/{id}")
    @PreAuthorize("hasAuthority('stock:item:list')")
    public Result<?> itemDelete(@PathVariable Long id) {
        stockService.delete(id);
        return Result.ok();
    }

    @GetMapping("/item/export")
    @PreAuthorize("hasAuthority('stock:item:list')")
    public void itemExport(HttpServletResponse resp) throws Exception {
        List<StockItem> list = stockService.itemListAll();

        Workbook wb = new XSSFWorkbook();
        Sheet s = wb.createSheet("耗材档案");
        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont(); headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        Row h = s.createRow(0);
        String[] headers = {"耗材编号", "名称", "实验室", "分类", "单位", "当前库存", "预警值"};
        for (int i = 0; i < headers.length; i++) {
            h.createCell(i).setCellValue(headers[i]);
            h.getCell(i).setCellStyle(headerStyle);
        }

        int r = 1;
        for (StockItem row : list) {
            Row rowObj = s.createRow(r++);
            rowObj.createCell(0).setCellValue(row.getCode() != null ? row.getCode() : "");
            rowObj.createCell(1).setCellValue(row.getName() != null ? row.getName() : "");
            rowObj.createCell(2).setCellValue(row.getLabName() != null ? row.getLabName() : "");
            rowObj.createCell(3).setCellValue(row.getCategory() != null ? row.getCategory() : "");
            rowObj.createCell(4).setCellValue(row.getUnit() != null ? row.getUnit() : "");
            rowObj.createCell(5).setCellValue(row.getQty() != null ? row.getQty().doubleValue() : 0.0);
            rowObj.createCell(6).setCellValue(row.getWarnQty() != null ? row.getWarnQty().doubleValue() : 0.0);
        }

        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition",
            "attachment; filename=" + URLEncoder.encode("耗材档案导出.xlsx", StandardCharsets.UTF_8));
        try (OutputStream out = resp.getOutputStream()) { wb.write(out); }
    }

    // ---------------- 流水 ----------------

    @GetMapping("/record/page")
    @PreAuthorize("hasAuthority('stock:record:list')")
    public Result<PageResult<StockRecord>> recordPage(@RequestParam(defaultValue = "1") int pageNum,
                                                      @RequestParam(defaultValue = "10") int pageSize,
                                                      @RequestParam(required = false) Long itemId,
                                                      @RequestParam(required = false) Integer type,
                                                      @RequestParam(required = false) Long reservationId) {
        return Result.ok(stockService.recordPage(pageNum, pageSize, itemId, type, reservationId));
    }

    @PostMapping("/record/in")
    @PreAuthorize("hasAuthority('stock:record:list')")
    public Result<Long> stockIn(@RequestBody StockRecordDTO dto) {
        fillCurrentUser(dto);
        return Result.ok(stockService.stockIn(dto));
    }

    @PostMapping("/record/out")
    @PreAuthorize("hasAuthority('stock:record:list')")
    public Result<Long> stockOut(@RequestBody StockRecordDTO dto) {
        fillCurrentUser(dto);
        return Result.ok(stockService.stockOut(dto));
    }

    private void fillCurrentUser(StockRecordDTO dto) {
        // 只要存在当前登录用户，就以当前登录用户为准覆盖经办人，
        // 忽略客户端传入的 userId，防止伪造经办人。
        LoginUser cu = SecurityUtil.current();
        if (cu != null && cu.getUser() != null) {
            dto.setUserId(cu.getUser().getId());
        }
    }

    @GetMapping("/record/export")
    @PreAuthorize("hasAuthority('stock:record:list')")
    public void recordExport(HttpServletResponse resp) throws Exception {
        List<StockRecord> list = stockService.recordListAll();

        Workbook wb = new XSSFWorkbook();
        Sheet s = wb.createSheet("出入库流水");
        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont(); headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        Row h = s.createRow(0);
        String[] headers = {"操作类型", "耗材名称", "操作人", "数量", "时间", "关联预约单"};
        for (int i = 0; i < headers.length; i++) {
            h.createCell(i).setCellValue(headers[i]);
            h.getCell(i).setCellStyle(headerStyle);
        }

        int r = 1;
        for (StockRecord row : list) {
            Row rowObj = s.createRow(r++);
            rowObj.createCell(0).setCellValue(typeText(row.getType()));
            rowObj.createCell(1).setCellValue(row.getItemName() != null ? row.getItemName() : "");
            rowObj.createCell(2).setCellValue(row.getOperatorName() != null ? row.getOperatorName() : "");
            rowObj.createCell(3).setCellValue(row.getQty() != null ? row.getQty().doubleValue() : 0.0);
            rowObj.createCell(4).setCellValue(row.getCreateTime() != null ? row.getCreateTime().toString() : "");
            rowObj.createCell(5).setCellValue(row.getReservationNo() != null ? row.getReservationNo() : "");
        }

        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition",
            "attachment; filename=" + URLEncoder.encode("出入库流水导出.xlsx", StandardCharsets.UTF_8));
        try (OutputStream out = resp.getOutputStream()) { wb.write(out); }
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
