package com.lab.module.stat.service;

import com.lab.module.stock.service.StockService;
import com.lab.security.DataScopeUtil;
import com.lab.security.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatService {

    private static final Logger log = LoggerFactory.getLogger(StatService.class);
    private static final DateTimeFormatter MMDD = DateTimeFormatter.ofPattern("MM-dd");

    private final JdbcTemplate jdbc;
    private final StockService stockService;

    @Autowired
    public StatService(JdbcTemplate jdbc, @Lazy StockService stockService) {
        this.jdbc = jdbc;
        this.stockService = stockService;
    }

    /**
     * 构建 lab_id 范围过滤子句。
     *
     * <p>规则：
     * <ul>
     *   <li>admin（scope = null）：返回空串，不加限</li>
     *   <li>scope 为空列表：返回 "AND 1=0"，什么也看不到（避免误查全部）</li>
     *   <li>scope 有值：返回 "AND lab_id IN (?, ?, ...)"，通过 params 注入占位参数</li>
     * </ul>
     */
    private String buildLabScopeSql(String column, List<Long> labIds, List<Object> params) {
        if (labIds == null) return "";
        if (labIds.isEmpty()) return "AND 1 = 0";
        params.addAll(labIds);
        return "AND " + column + " IN (" + String.join(",",
                java.util.Collections.nCopies(labIds.size(), "?")) + ")";
    }

    // ============================================================
    // 1. 工作台总览
    // ============================================================
    /** 工作台总览 — 每次都从数据库实时读取，确保数据实时更新。 */
    public Map<String, Object> overview() {
        Map<String, Object> m = new LinkedHashMap<>();
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();

        // 今日预约
        {
            List<Object> params = new ArrayList<>();
            String extra = buildLabScopeSql("lab_id", scope, params);
            String sql = "SELECT COUNT(*) FROM lab_reservation WHERE deleted = 0 " +
                    "AND start_time >= CURDATE() AND start_time < CURDATE() + INTERVAL 1 DAY " + extra;
            m.put("todayReservations", countOrZero(sql, params));
        }

        // 待审核预约
        {
            List<Object> params = new ArrayList<>();
            String extra = buildLabScopeSql("lab_id", scope, params);
            String sql = "SELECT COUNT(*) FROM lab_reservation WHERE deleted = 0 AND status = 0 " + extra;
            m.put("pendingReservations", countOrZero(sql, params));
        }

        // 实验室总数
        {
            List<Object> params = new ArrayList<>();
            String extra = buildLabScopeSql("id", scope, params);
            String sql = "SELECT COUNT(*) FROM lab_room WHERE deleted = 0 " + extra;
            m.put("labCount", countOrZero(sql, params));
        }

        // 设备总数
        {
            List<Object> params = new ArrayList<>();
            String extra = buildLabScopeSql("lab_id", scope, params);
            String sql = "SELECT COUNT(*) FROM lab_device WHERE deleted = 0 " + extra;
            m.put("deviceCount", countOrZero(sql, params));
        }

        // 待处理维修（关联设备表找 lab_id）
        {
            List<Object> params = new ArrayList<>();
            String extra = buildLabScopeSql("d.lab_id", scope, params);
            String sql = "SELECT COUNT(*) FROM lab_device_repair r LEFT JOIN lab_device d ON d.id = r.device_id " +
                    "WHERE r.status IN (0, 1) " + extra;
            m.put("pendingRepairs", countOrZero(sql, params));
        }

        // 库存预警（由 StockService 内部已按 scope 过滤）
        m.put("stockWarnings", stockService.countWarning());

        // 用户统计仅对管理员有意义；非管理员返回 0 / 空
        if (scope == null) {
            m.put("userCount", countOrZero("SELECT COUNT(*) FROM sys_user WHERE deleted = 0"));
            m.put("todayNewUsers", countOrZero(
                    "SELECT COUNT(*) FROM sys_user WHERE deleted = 0 " +
                            "AND create_time >= CURDATE() AND create_time < CURDATE() + INTERVAL 1 DAY"));
            m.put("pendingTeacherCount", countOrZero(
                    "SELECT COUNT(DISTINCT u.id) FROM sys_user u " +
                            "INNER JOIN sys_user_role ur ON ur.user_id = u.id " +
                            "INNER JOIN sys_role r ON r.id = ur.role_id " +
                            "WHERE u.deleted = 0 AND u.status = 0 AND r.role_code = 'ROLE_TEACHER'"));
            m.put("pendingTeacherList", jdbc.queryForList(
                    "SELECT u.id, u.username, u.real_name AS realName, u.phone, u.email, " +
                            "u.create_time AS createTime, d.name AS deptName " +
                            "FROM sys_user u " +
                            "INNER JOIN sys_user_role ur ON ur.user_id = u.id " +
                            "INNER JOIN sys_role r ON r.id = ur.role_id " +
                            "LEFT JOIN sys_dept d ON d.id = u.dept_id " +
                            "WHERE u.deleted = 0 AND u.status = 0 AND r.role_code = 'ROLE_TEACHER' " +
                            "ORDER BY u.create_time DESC LIMIT 5"));
        } else {
            m.put("userCount", 0);
            m.put("todayNewUsers", 0);
            m.put("pendingTeacherCount", 0);
            m.put("pendingTeacherList", new ArrayList<Map<String, Object>>());
        }

        // 最近 5 条预约（按 scope 过滤）
        {
            List<Object> params = new ArrayList<>();
            String extra = buildLabScopeSql("r.lab_id", scope, params);
            String sql = "SELECT r.id, r.reserve_no AS reserveNo, r.purpose, r.status, " +
                    "r.start_time AS startTime, r.end_time AS endTime, r.create_time AS createTime, " +
                    "lr.name AS labName, u.real_name AS applicant " +
                    "FROM lab_reservation r LEFT JOIN lab_room lr ON lr.id = r.lab_id " +
                    "LEFT JOIN sys_user u ON u.id = r.user_id " +
                    "WHERE r.deleted = 0 " + extra + " ORDER BY r.create_time DESC LIMIT 5";
            m.put("recentReservations", jdbc.queryForList(sql, params.toArray()));
        }

        // 库存预警 Top 5（StockService 内部按 scope 过滤）
        m.put("stockWarningList", stockService.warningList(5));

        // 近 7 天预约趋势（按 scope 过滤）
        {
            List<Object> params = new ArrayList<>();
            String extra = buildLabScopeSql("lab_id", scope, params);
            String sql = "SELECT DATE(start_time) AS day, COUNT(*) AS count FROM lab_reservation " +
                    "WHERE deleted = 0 AND start_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
                    extra + " GROUP BY DATE(start_time)";
            List<Map<String, Object>> raw = jdbc.queryForList(sql, params.toArray());
            Map<String, Number> hit = new HashMap<>();
            for (Map<String, Object> row : raw) {
                Object day = row.get("day");
                String key = day == null ? "" : day.toString();
                hit.put(key, (Number) row.get("count"));
            }
            List<Map<String, Object>> dailyReservations = new ArrayList<>(7);
            LocalDate today = LocalDate.now();
            for (int i = 6; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                String key = d.toString();
                Number n = hit.get(key);
                int count = n == null ? 0 : n.intValue();
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("day", key);
                point.put("label", d.format(MMDD));
                point.put("count", count);
                dailyReservations.add(point);
            }
            m.put("dailyReservations", dailyReservations);
        }

        // 设备状态分布（按 scope 过滤）
        {
            List<Object> params = new ArrayList<>();
            String extra = buildLabScopeSql("lab_id", scope, params);
            String sql = "SELECT status, COUNT(*) AS count FROM lab_device WHERE deleted = 0 " +
                    extra + " GROUP BY status";
            m.put("deviceStatus", jdbc.queryForList(sql, params.toArray()));
        }

        // 实验室预约次数 TOP 5（按 scope 过滤）
        {
            List<Object> params = new ArrayList<>();
            String extra = buildLabScopeSql("lr.id", scope, params);
            String sql = "SELECT lr.id, lr.name AS labName, COUNT(r.id) AS reserveCount " +
                    "FROM lab_room lr LEFT JOIN lab_reservation r ON r.lab_id = lr.id AND r.deleted = 0 " +
                    "WHERE lr.deleted = 0 " + extra + " GROUP BY lr.id, lr.name " +
                    "ORDER BY reserveCount DESC LIMIT 5";
            m.put("labRanking", jdbc.queryForList(sql, params.toArray()));
        }

        // 维修单状态分布（按 scope 过滤）
        {
            List<Object> params = new ArrayList<>();
            String extra = buildLabScopeSql("d.lab_id", scope, params);
            String sql = "SELECT r.status, COUNT(*) AS count FROM lab_device_repair r " +
                    "LEFT JOIN lab_device d ON d.id = r.device_id WHERE 1=1 " + extra + " GROUP BY r.status";
            m.put("repairStatus", jdbc.queryForList(sql, params.toArray()));
        }

        // 角色分布：仅管理员可见
        if (scope == null) {
            m.put("roleDistribution", jdbc.queryForList(
                    "SELECT r.id, r.role_code AS roleCode, r.role_name AS roleName, " +
                            "COUNT(DISTINCT u.id) AS userCount FROM sys_role r " +
                            "LEFT JOIN sys_user_role ur ON ur.role_id = r.id " +
                            "LEFT JOIN sys_user u ON u.id = ur.user_id AND u.deleted = 0 " +
                            "WHERE r.deleted = 0 GROUP BY r.id, r.role_code, r.role_name " +
                            "ORDER BY userCount DESC"));
        } else {
            m.put("roleDistribution", new ArrayList<Map<String, Object>>());
        }

        // ========= 个人维度数据：只要登录就无条件写入（所有角色共享同一套字段）
        Long uid = SecurityUtil.currentUserId();
        if (uid != null) {
            List<Object> uidParam = List.of(uid);
            m.put("myReservationCount", countOrZero(
                    "SELECT COUNT(*) FROM lab_reservation WHERE deleted = 0 AND user_id = ?", uidParam));
            m.put("myPendingReservations", countOrZero(
                    "SELECT COUNT(*) FROM lab_reservation WHERE deleted = 0 AND status = 0 AND user_id = ?", uidParam));
            m.put("checkInCount", countOrZero(
                    "SELECT COUNT(*) FROM lab_reservation WHERE deleted = 0 AND check_in_time IS NOT NULL AND user_id = ?", uidParam));
            m.put("myRepairCount", countOrZero(
                    "SELECT COUNT(*) FROM lab_device_repair WHERE reporter_id = ?", uidParam));
            m.put("myPendingRepairs", countOrZero(
                    "SELECT COUNT(*) FROM lab_device_repair WHERE status IN (0, 1) AND reporter_id = ?", uidParam));
            m.put("myReservationList", jdbc.queryForList(
                    "SELECT r.id, r.reserve_no AS reserveNo, r.purpose, r.status, " +
                            "r.start_time AS startTime, r.end_time AS endTime, " +
                            "lr.name AS labName FROM lab_reservation r " +
                            "LEFT JOIN lab_room lr ON lr.id = r.lab_id " +
                            "WHERE r.deleted = 0 AND r.user_id = ? " +
                            "ORDER BY r.create_time DESC LIMIT 10", uid));
            m.put("myRepairList", jdbc.queryForList(
                    "SELECT rep.id, rep.status, rep.fault_desc AS faultDesc, " +
                            "rep.report_time AS reportTime, " +
                            "d.name AS deviceName, lr.name AS labName " +
                            "FROM lab_device_repair rep " +
                            "LEFT JOIN lab_device d ON d.id = rep.device_id " +
                            "LEFT JOIN lab_room lr ON lr.id = d.lab_id " +
                            "WHERE rep.reporter_id = ? " +
                            "ORDER BY rep.report_time DESC LIMIT 10", uid));
            // LABADMIN：额外补「我管理的实验室待审核预约数」
            if (DataScopeUtil.isLabAdmin()) {
                List<Long> myLabs = DataScopeUtil.getLabIdsForLabAdmin();
                if (myLabs != null && !myLabs.isEmpty()) {
                    String labScope = String.join(",", myLabs.stream().map(String::valueOf).toArray(String[]::new));
                    m.put("myPendingReservations", jdbc.queryForObject(
                            "SELECT COUNT(*) FROM lab_reservation WHERE deleted = 0 AND status = 0 AND lab_id IN (" + labScope + ")", Integer.class));
                }
            }
        } else {
            m.put("myReservationCount", 0);
            m.put("myPendingReservations", 0);
            m.put("myRepairCount", 0);
            m.put("myPendingRepairs", 0);
            m.put("checkInCount", 0);
            m.put("myReservationList", new ArrayList<Map<String, Object>>());
            m.put("myRepairList", new ArrayList<Map<String, Object>>());
        }

        return m;
    }

    // ============================================================
    // 2. 实验室使用率分析
    // ============================================================
    public Map<String, Object> labUsage() {
        Map<String, Object> m = new LinkedHashMap<>();
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();

        // ranking
        {
            List<Object> params = new ArrayList<>();
            String extra = buildLabScopeSql("lr.id", scope, params);
            String sql = "SELECT lr.id, lr.name AS labName, COUNT(r.id) AS reserveCount, " +
                    "COALESCE(ROUND(SUM(CASE WHEN r.status IN (1,3,4) " +
                    "THEN TIMESTAMPDIFF(MINUTE, r.start_time, r.end_time) ELSE 0 END) / 60.0, 1), 0) AS useHours " +
                    "FROM lab_room lr LEFT JOIN lab_reservation r ON r.lab_id = lr.id AND r.deleted = 0 " +
                    "WHERE lr.deleted = 0 " + extra + " GROUP BY lr.id, lr.name " +
                    "ORDER BY reserveCount DESC, useHours DESC LIMIT 10";
            m.put("ranking", jdbc.queryForList(sql, params.toArray()));
        }

        // trend
        {
            List<Object> params = new ArrayList<>();
            String extra = buildLabScopeSql("lab_id", scope, params);
            String sql = "SELECT DATE(start_time) AS day, COUNT(*) AS cnt FROM lab_reservation " +
                    "WHERE deleted = 0 AND start_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
                    extra + " GROUP BY DATE(start_time)";
            List<Map<String, Object>> raw = jdbc.queryForList(sql, params.toArray());
            Map<String, Number> hit = new HashMap<>();
            for (Map<String, Object> row : raw) {
                Object day = row.get("day");
                String key = day == null ? "" : day.toString();
                hit.put(key, (Number) row.get("cnt"));
            }
            List<Map<String, Object>> trend = new ArrayList<>(7);
            List<String> labels = new ArrayList<>(7);
            LocalDate today = LocalDate.now();
            for (int i = 6; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                String key = d.toString();
                Number n = hit.get(key);
                int count = n == null ? 0 : n.intValue();
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("day", key);
                point.put("count", count);
                trend.add(point);
                labels.add(d.format(MMDD));
            }
            m.put("trend", trend);
            m.put("labels", labels);
        }

        // monthTotal
        {
            List<Object> params = new ArrayList<>();
            String extra = buildLabScopeSql("lab_id", scope, params);
            String sql = "SELECT COUNT(*) FROM lab_reservation WHERE deleted = 0 " +
                    "AND start_time >= DATE_FORMAT(CURDATE(), '%Y-%m-01') " +
                    "AND start_time < DATE_FORMAT(CURDATE(), '%Y-%m-01') + INTERVAL 1 MONTH " + extra;
            m.put("monthTotal", countOrZero(sql, params));
        }

        return m;
    }

    // ============================================================
    // 3. 设备故障分析
    // ============================================================
    public Map<String, Object> deviceFault() {
        Map<String, Object> m = new LinkedHashMap<>();
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();

        // deviceStatus
        {
            List<Object> params = new ArrayList<>();
            String extra = buildLabScopeSql("lab_id", scope, params);
            String sql = "SELECT status, COUNT(*) AS count, " +
                    "CASE status WHEN 1 THEN '在用' WHEN 2 THEN '维修' WHEN 3 THEN '报废' ELSE '未知' END AS statusName " +
                    "FROM lab_device WHERE deleted = 0 " + extra + " GROUP BY status";
            m.put("deviceStatus", jdbc.queryForList(sql, params.toArray()));
        }

        // repairStatus
        {
            List<Object> params = new ArrayList<>();
            String extra = buildLabScopeSql("d.lab_id", scope, params);
            String sql = "SELECT r.status, COUNT(*) AS count, " +
                    "CASE r.status WHEN 0 THEN '待指派' WHEN 1 THEN '处理中' WHEN 2 THEN '已完成' " +
                    "WHEN 3 THEN '已驳回' ELSE '未知' END AS statusName " +
                    "FROM lab_device_repair r LEFT JOIN lab_device d ON d.id = r.device_id WHERE 1=1 " +
                    extra + " GROUP BY r.status";
            m.put("repairStatus", jdbc.queryForList(sql, params.toArray()));
        }

        // pendingList
        {
            List<Object> params = new ArrayList<>();
            String extra = buildLabScopeSql("d.lab_id", scope, params);
            String sql = "SELECT r.id, r.fault_desc AS faultDesc, r.status, r.report_time AS reportTime, " +
                    "r.handle_note AS handleNote, d.asset_no AS assetNo, d.name AS deviceName " +
                    "FROM lab_device_repair r LEFT JOIN lab_device d ON d.id = r.device_id " +
                    "WHERE r.status IN (0, 1) " + extra + " ORDER BY r.report_time DESC LIMIT 10";
            m.put("pendingList", jdbc.queryForList(sql, params.toArray()));
        }

        return m;
    }

    // ============================================================
    // 4. 耗材库存预警
    // ============================================================
    public Map<String, Object> stockWarning() {
        Map<String, Object> m = new LinkedHashMap<>();
        // StockService 内部已按 scope 过滤，此处直接调用
        m.put("warningList", stockService.warningList(null));
        m.put("topUsage", stockService.topUsage(10));
        m.put("inQty", stockService.sumInQty());
        m.put("outQty", stockService.sumOutQty());
        return m;
    }

    // ============================================================
    // helpers
    // ============================================================
    private int countOrZero(String sql) {
        Integer v = jdbc.queryForObject(sql, Integer.class);
        return v == null ? 0 : v;
    }

    private int countOrZero(String sql, List<Object> params) {
        Integer v = jdbc.queryForObject(sql, Integer.class, params.toArray());
        return v == null ? 0 : v;
    }
}
