package com.lab.common;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Excel 导出工具，统一管理工作簿创建、样式、表头与数据写入，
 * 避免各 Controller 中重复编写 Workbook / Sheet / CellStyle 代码。
 *
 * <p>典型用法：
 * <pre>
 * ExcelExportUtil.export("我的数据.xlsx", resp,
 *     new String[]{"列1", "列2", "列3"},
 *     dataList,
 *     (row, item) -> {
 *         row.createCell(0).setCellValue(item.getName());
 *         row.createCell(1).setCellValue(item.getValue());
 *         row.createCell(2).setCellValue(item.getRemark());
 *     });
 * </pre>
 */
public final class ExcelExportUtil {

    private ExcelExportUtil() {}

    /**
     * 导出 Excel 文件，由调用方填充每一行数据。
     *
     * @param filename 导出文件名（含 .xlsx 后缀）
     * @param resp    HttpServletResponse
     * @param headers 表头列名
     * @param data    数据集合
     * @param rowWriter 行填充逻辑 (row, item) -> void
     */
    public static <T> void export(String filename,
                                   HttpServletResponse resp,
                                   String[] headers,
                                   List<T> data,
                                   BiConsumer<Row, T> rowWriter) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            CellStyle headerStyle = buildHeaderStyle(wb);
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            if (data != null) {
                int rowIdx = 1;
                for (T item : data) {
                    Row row = sheet.createRow(rowIdx++);
                    rowWriter.accept(row, item);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }

            writeToResponse(wb, filename, resp);
        }
    }

    /**
     * 简化版：使用函数数组作为列提取器。
     */
    @SafeVarargs
    public static <T> void export(String filename,
                                   HttpServletResponse resp,
                                   String[] headers,
                                   List<T> data,
                                   Function<T, Object>... colExtractors) throws IOException {
        export(filename, resp, headers, data, (row, item) -> {
            for (int i = 0; i < colExtractors.length; i++) {
                Object v = colExtractors[i].apply(item);
                if (v == null) continue;
                if (v instanceof Number n) {
                    row.createCell(i).setCellValue(n.doubleValue());
                } else {
                    row.createCell(i).setCellValue(v.toString());
                }
            }
        });
    }

    /**
     * 直接写入已构造好的 Workbook（保留复杂布局场景）。
     */
    public static void writeWorkbook(String filename, Workbook wb, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
        try (OutputStream out = resp.getOutputStream()) {
            wb.write(out);
        }
    }

    private static CellStyle buildHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static void writeToResponse(Workbook wb, String filename, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
        try (OutputStream out = resp.getOutputStream()) {
            wb.write(out);
        }
    }
}
