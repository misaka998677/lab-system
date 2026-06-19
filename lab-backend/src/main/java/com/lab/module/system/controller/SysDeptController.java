package com.lab.module.system.controller;

import com.lab.common.Result;
import com.lab.config.StatCacheInvalidator;
import com.lab.module.system.entity.SysDept;
import com.lab.module.system.service.SysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/dept")
public class SysDeptController {
    private final SysDeptService deptService;
    private final StatCacheInvalidator cache;

    @Autowired
    public SysDeptController(SysDeptService s, StatCacheInvalidator c) { this.deptService = s; this.cache = c; }

    @GetMapping("/all")
    public Result<List<SysDept>> all() { return Result.ok(deptService.all()); }

    @GetMapping("/tree")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> tree() { return Result.ok(deptService.tree()); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> create(@RequestBody SysDept d) {
        Long id = deptService.create(d);
        cache.evictOverview();
        return Result.ok(id);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> update(@RequestBody SysDept d) {
        deptService.update(d);
        cache.evictOverview();
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> delete(@PathVariable Long id) {
        deptService.delete(id);
        cache.evictOverview();
        return Result.ok();
    }
}
