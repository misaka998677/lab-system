package com.lab.module.system.controller;

import com.lab.common.Result;
import com.lab.config.StatCacheInvalidator;
import com.lab.module.system.entity.SysMenu;
import com.lab.module.system.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/menu")
public class SysMenuController {
    private final SysMenuService menuService;
    private final StatCacheInvalidator cache;

    @Autowired
    public SysMenuController(SysMenuService s, StatCacheInvalidator c) { this.menuService = s; this.cache = c; }

    @GetMapping("/tree")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<SysMenu>> tree() { return Result.ok(menuService.tree()); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> create(@RequestBody SysMenu m) {
        Long id = menuService.create(m);
        cache.evictOverview();
        return Result.ok(id);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> update(@RequestBody SysMenu m) {
        menuService.update(m);
        cache.evictOverview();
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> delete(@PathVariable Long id) {
        menuService.delete(id);
        cache.evictOverview();
        return Result.ok();
    }
}
