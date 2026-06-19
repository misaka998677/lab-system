package com.lab.module.system.controller;

import com.lab.common.ExcelExportUtil;
import com.lab.common.PageParam;
import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.config.StatCacheInvalidator;
import com.lab.module.system.entity.SysUser;
import com.lab.module.system.service.SysUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/user")
public class SysUserController {

    private final SysUserService userService;
    private final StatCacheInvalidator cache;

    @Autowired
    public SysUserController(SysUserService s, StatCacheInvalidator c) { this.userService = s; this.cache = c; }

    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<SysUser>> page(@RequestParam(defaultValue = "1")  int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long deptId,
                                            @RequestParam(required = false) Integer status) {
        int[] p = PageParam.clamp(pageNum, pageSize);
        return Result.ok(userService.page(p[0], p[1], keyword, deptId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysUser> detail(@PathVariable Long id) { return Result.ok(userService.detail(id)); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> create(@RequestBody Map<String, Object> body) {
        SysUser u = mapTo(body);
        @SuppressWarnings("unchecked") List<Long> roleIds = (List<Long>) body.get("roleIds");
        Long id = userService.create(u, roleIds);
        cache.evictOverview();
        return Result.ok(id);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> update(@RequestBody Map<String, Object> body) {
        SysUser u = mapTo(body);
        u.setId(((Number) body.get("id")).longValue());
        @SuppressWarnings("unchecked") List<Long> roleIds = (List<Long>) body.get("roleIds");
        userService.update(u, roleIds);
        cache.evictOverview();
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> delete(@PathVariable Long id) {
        userService.delete(id);
        cache.evictOverview();
        return Result.ok();
    }

    @PutMapping("/{id}/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> status(@PathVariable Long id, @PathVariable Integer status) {
        userService.changeStatus(id, status);
        cache.evictOverview();
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> resetPwd(@PathVariable Long id, @RequestParam String password) {
        userService.resetPassword(id, password);
        cache.evictOverview();
        return Result.ok();
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void export(HttpServletResponse resp) throws Exception {
        List<SysUser> users = userService.listAll();
        ExcelExportUtil.export("用户列表.xlsx", resp,
                new String[]{"ID", "用户名", "真实姓名", "手机", "邮箱", "部门", "状态", "创建时间"},
                users,
                (row, u) -> {
                    row.createCell(0).setCellValue(u.getId());
                    row.createCell(1).setCellValue(u.getUsername());
                    row.createCell(2).setCellValue(u.getRealName());
                    row.createCell(3).setCellValue(u.getPhone());
                    row.createCell(4).setCellValue(u.getEmail());
                    row.createCell(5).setCellValue(u.getDeptName());
                    row.createCell(6).setCellValue(u.getStatus() == 1 ? "正常" : "禁用");
                    row.createCell(7).setCellValue(u.getCreateTime() != null ? u.getCreateTime().toString() : "");
                });
    }

    @SuppressWarnings("unchecked")
    private SysUser mapTo(Map<String, Object> body) {
        SysUser u = new SysUser();
        u.setUsername((String) body.get("username"));
        u.setPassword((String) body.get("password"));
        u.setRealName((String) body.get("realName"));
        u.setPhone((String) body.get("phone"));
        u.setEmail((String) body.get("email"));
        if (body.get("gender") != null) u.setGender(((Number) body.get("gender")).intValue());
        if (body.get("status") != null) u.setStatus(((Number) body.get("status")).intValue());
        if (body.get("deptId") != null) u.setDeptId(((Number) body.get("deptId")).longValue());
        u.setRemark((String) body.get("remark"));
        return u;
    }
}
