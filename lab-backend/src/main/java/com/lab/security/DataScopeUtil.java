package com.lab.security;

import com.lab.module.system.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 数据范围工具。
 *
 * <p>根据当前登录用户角色，计算其可访问的实验室 ID 列表，用于 Service 层注入查询条件。
 *
 * <ul>
 *   <li>ROLE_ADMIN：返回 null，表示不限制（查看全部）</li>
 *   <li>ROLE_LABADMIN：返回自己管理的实验室 ID 列表（lab_room.manager_id = 当前用户）</li>
 *   <li>ROLE_TEACHER：返回自己学院下所有实验室的 ID 列表（用于审核范围限制）</li>
 *   <li>其他角色（学生等）：返回 null，查看公开数据，个人数据由 Service 层单独按 userId 过滤</li>
 * </ul>
 */
@Component
public class DataScopeUtil {

    private static SysUserMapper userMapper;

    @Autowired
    public DataScopeUtil(SysUserMapper m) { userMapper = m; }

    /**
     * 健壮的角色匹配：忽略大小写，自动补齐/去除 ROLE_ 前缀。
     * 如 roles=["student"]，target="ROLE_STUDENT" 也能命中；反之亦然。
     */
    private static boolean hasRole(LoginUser u, String target) {
        if (u == null || target == null) return false;
        Set<String> roles = u.getRoles();
        if (roles == null || roles.isEmpty()) return false;
        String t = target.toUpperCase(Locale.ROOT).replace("ROLE_", "");
        for (String r : roles) {
            if (r == null) continue;
            String n = r.toUpperCase(Locale.ROOT).replace("ROLE_", "");
            if (n.equals(t)) return true;
        }
        return false;
    }

    public static boolean isAdmin()    { return hasRole(SecurityUtil.current(), "ROLE_ADMIN"); }
    public static boolean isLabAdmin() { return hasRole(SecurityUtil.current(), "ROLE_LABADMIN"); }
    public static boolean isTeacher()  { return hasRole(SecurityUtil.current(), "ROLE_TEACHER"); }
    public static boolean isStudent()  { return hasRole(SecurityUtil.current(), "ROLE_STUDENT"); }

    /**
     * 获取当前 LABADMIN 可访问的实验室 ID 列表。
     */
    public static List<Long> getLabIdsForLabAdmin() {
        LoginUser u = SecurityUtil.current();
        if (u == null) return Collections.emptyList();
        if (hasRole(u, "ROLE_ADMIN")) return null;
        if (!hasRole(u, "ROLE_LABADMIN")) return Collections.emptyList();
        List<Long> ids = userMapper.findManagedLabIds(u.getUser().getId());
        return ids == null || ids.isEmpty() ? Collections.emptyList() : ids;
    }

    /**
     * 获取当前 TEACHER 可审核的实验室 ID 列表（自己学院下的所有实验室）。
     */
    public static List<Long> getLabIdsForTeacher() {
        LoginUser u = SecurityUtil.current();
        if (u == null) return Collections.emptyList();
        if (hasRole(u, "ROLE_ADMIN")) return null;
        if (hasRole(u, "ROLE_LABADMIN")) {
            List<Long> ids = userMapper.findManagedLabIds(u.getUser().getId());
            return ids == null || ids.isEmpty() ? Collections.emptyList() : ids;
        }
        if (hasRole(u, "ROLE_TEACHER")) {
            Long deptId = userMapper.findDeptIdByUserId(u.getUser().getId());
            if (deptId == null) return Collections.emptyList();
            List<Long> ids = userMapper.findLabIdsByDeptId(deptId);
            return ids == null || ids.isEmpty() ? Collections.emptyList() : ids;
        }
        return null;
    }

    public static Long getCurrentDeptId() {
        LoginUser u = SecurityUtil.current();
        if (u == null) return null;
        return userMapper.findDeptIdByUserId(u.getUser().getId());
    }

    public static String currentScopeKey() {
        LoginUser u = SecurityUtil.current();
        if (u == null) return "anon";
        if (hasRole(u, "ROLE_ADMIN")) return "admin";
        return "u_" + u.getUser().getId();
    }

    public static boolean isLabAllowed(Long labId, List<Long> allowedLabIds) {
        if (labId == null) return false;
        if (allowedLabIds == null) return true;
        return allowedLabIds.contains(labId);
    }
}
