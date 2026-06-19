package com.lab.common;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入结果。
 *
 * <p>对每一行 Excel 数据做独立校验，失败行附带行号与原因，
 * 以便前端定位展示；成功行写入数据库。若存在任何失败行，
 * 整体事务回滚，确保不会写入部分数据。
 */
@Data
public class ImportResult {
    private int total;
    private int successCount;
    private int failCount;
    private List<FailRow> failRows = new ArrayList<>();

    /** 当存在 {@link #failRows} 时设为 true，表示应回滚事务并返回前端展示。 */
    public boolean hasFailure() { return !failRows.isEmpty(); }

    public void incSuccess() { this.successCount++; }

    public void addFail(int rowNum, String reason) {
        this.failCount++;
        FailRow r = new FailRow();
        r.rowNum = rowNum;
        r.reason = reason;
        this.failRows.add(r);
    }

    @Data
    public static class FailRow {
        private int rowNum;
        private String reason;
    }
}
