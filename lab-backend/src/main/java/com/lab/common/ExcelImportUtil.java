package com.lab.common;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Excel 导入与模板生成工具。
 *
 * <p>约定：
 * <ul>
 *   <li>Sheet 0 为目标数据；第一行为表头。</li>
 *   <li>数据从第 2 行（rowIndex = 1）开始读取，跳过空行。</li>
 *   <li>由调用方通过 {@code rowToEntity} 把一行 Map 转为业务实体并做校验，
 *       失败时通过 {@link ImportResult#addFail(int, String)} 登记失败原因。</li>
 * </ul>
 */
public final class ExcelImportUtil {

    private ExcelImportUtil() {}

    /**
     * 写一个简单的模板文件：表头加粗 + 一行示例数据。
     *
     * @param out      输出流（通常是 HttpServletResponse#getOutputStream）
     * @param headers  中文表头，按顺序写入
     * @param sample   可选的示例数据行；可为 null 表示不写示例行
     */
    public static void writeTemplate(OutputStream out, String[] headers, String[] sample) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }
            if (sample != null && sample.length > 0) {
                Row sampleRow = sheet.createRow(1);
                for (int i = 0; i < sample.length; i++) {
                    sampleRow.createCell(i).setCellValue(sample[i]);
                }
            }
            for (int i = 0; i < headers.length; i++) sheet.setColumnWidth(i, 20 * 256);
            wb.write(out);
        }
    }

    /**
     * 读取 Excel 第一行（表头），返回每列中文列名。
     */
    public static List<String> readHeader(Row headerRow) {
        List<String> headers = new ArrayList<>();
        if (headerRow == null) return headers;
        short last = headerRow.getLastCellNum();
        for (int i = 0; i < last; i++) {
            Cell c = headerRow.getCell(i);
            headers.add(c == null ? "" : cellToString(c).trim());
        }
        return headers;
    }

    /**
     * 读取 Excel 数据行（从第 2 行开始）。
     *
     * @param headers     从表头解析得到的列名列表（顺序）
     * @param rowToEntity 把 Map(列名, 值) 转为业务实体或登记错误。
     *                    该函数返回 null 代表该行被跳过（例如空行）；
     *                    非 null 代表通过，会被累加到返回列表中。
     * @return 合法实体列表
     */
    public static <T> List<T> parseRows(Sheet sheet,
                                         List<String> headers,
                                         ImportResult result,
                                         Function<Map<String, String>, T> rowToEntity) {
        List<T> items = new ArrayList<>();
        int totalDataRows = 0;
        for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) continue;
            Map<String, String> rowMap = new LinkedHashMap<>();
            boolean anyValue = false;
            for (int cIdx = 0; cIdx < headers.size(); cIdx++) {
                Cell c = row.getCell(cIdx);
                String val = cellToString(c).trim();
                rowMap.put(headers.get(cIdx), val);
                if (!val.isEmpty()) anyValue = true;
            }
            if (!anyValue) continue; // 空行跳过
            totalDataRows++;
            T entity = rowToEntity.apply(rowMap);
            if (entity != null) items.add(entity);
        }
        result.setTotal(totalDataRows);
        result.setSuccessCount(items.size());
        result.setFailCount(result.getFailRows().size());
        return items;
    }

    /**
     * 从输入流读取第一页 Sheet；调用方负责关闭流。
     */
    public static Sheet readFirstSheet(InputStream in) throws IOException {
        Workbook wb = new XSSFWorkbook(in);
        return wb.getSheetAt(0);
    }

    /** 将任意类型单元格安全地转为字符串（含数字/日期/公式处理）。 */
    public static String cellToString(Cell c) {
        if (c == null) return "";
        switch (c.getCellType()) {
            case STRING: return c.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(c)) {
                    // 日期格式化（简单处理，避免使用 SimpleDateFormat 外部依赖）
                    return String.valueOf(c.getDateCellValue().getTime());
                }
                double v = c.getNumericCellValue();
                if (v == Math.floor(v) && !Double.isInfinite(v)) {
                    return String.valueOf((long) v);
                }
                return String.valueOf(v);
            case BOOLEAN: return String.valueOf(c.getBooleanCellValue());
            case FORMULA:
                try { return String.valueOf(c.getNumericCellValue()); } catch (Exception e) { return c.getStringCellValue(); }
            default: return "";
        }
    }

    /** 空值检查 + trim；若为空则抛出 BizException，用于登记必填字段缺失。 */
    public static String require(Map<String, String> row, String column) {
        String v = row.get(column);
        if (v == null || v.isBlank()) {
            throw new BizException("缺少必填列：" + column);
        }
        return v.trim();
    }

    public static String optional(Map<String, String> row, String column) {
        String v = row.get(column);
        return v == null ? "" : v.trim();
    }

    public static Integer toInteger(Map<String, String> row, String column, Integer defaultValue) {
        String v = row.get(column);
        if (v == null || v.isBlank()) return defaultValue;
        try {
            return (int) Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            throw new BizException("列「" + column + "」必须是数字");
        }
    }
}
