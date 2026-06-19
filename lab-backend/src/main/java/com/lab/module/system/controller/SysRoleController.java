package com.lab.module.system.controller;

import com.lab.common.PageParam;
import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.config.StatCacheInvalidator;
import com.lab.module.system.entity.SysRole;
import com.lab.module.system.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/role")
public class SysRoleController {

    private final SysRoleService roleService;
    private final StatCacheInvalidator cache;

    @Autowired
    public SysRoleController(SysRoleService s, StatCacheInvalidator c) { this.roleService = s; this.cache = c; }

    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<SysRole>> page(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) String keyword) {
        int[] p = PageParam.clamp(pageNum, pageSize);
        return Result.ok(roleService.page(p[0], p[1], keyword));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> all() { return Result.ok(roleService.all()); }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SysRole> detail(@PathVariable Long id) { return Result.ok(roleService.detail(id)); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> create(@RequestBody Map<String, Object> body) {
        Long id = roleService.create(toRole(body), idList(body.get("menuIds")));
        cache.evictOverview();
        return Result.ok(id);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> update(@RequestBody Map<String, Object> body) {
        SysRole r = toRole(body);
        r.setId(((Number) body.get("id")).longValue());
        roleService.update(r, idList(body.get("menuIds")));
        cache.evictOverview();
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> delete(@PathVariable Long id) {
        roleService.delete(id);
        cache.evictOverview();
        return Result.ok();
    }

    private SysRole toRole(Map<String, Object> b) {
        SysRole r = new SysRole();
        r.setRoleCode((String) b.get("roleCode"));
        r.setRoleName((String) b.get("roleName"));
        Object sortVal = b.get("sortNo") != null ? b.get("sortNo") : b.get("sort");
        if (sortVal != null) r.setSortNo(((Number) sortVal).intValue());
        if (b.get("status") != null) r.setStatus(((Number) b.get("status")).intValue());
        r.setRemark((String) b.get("remark"));
        return r;
    }

    @SuppressWarnings("unchecked")
    private List<Long> idList(Object o) {
        if (!(o instanceof List<?> l)) return java.util.Collections.emptyList();
        return l.stream().filter(java.util.Objects::nonNull)
                .map(x -> ((Number) x).longValue()).toList();
    }
}
