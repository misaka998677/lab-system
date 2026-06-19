package com.lab.module.lab.controller;

import com.lab.common.PageParam;
import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.config.StatCacheInvalidator;
import com.lab.module.lab.entity.LabRoom;
import com.lab.module.lab.service.LabRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lab/room")
public class LabRoomController {
    private final LabRoomService roomService;
    private final StatCacheInvalidator cache;

    @Autowired
    public LabRoomController(LabRoomService s, StatCacheInvalidator c) { this.roomService = s; this.cache = c; }

    /** 实验室列表（所有登录用户均可查看，用于实验室档案与预约选择） */
    @GetMapping("/page")
    public Result<PageResult<LabRoom>> page(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long deptId,
                                            @RequestParam(required = false) Integer status) {
        int[] p = PageParam.clamp(pageNum, pageSize);
        return Result.ok(roomService.page(p[0], p[1], keyword, deptId, status));
    }

    /** 所有实验室列表（预约时下拉使用，所有登录用户可查看） */
    @GetMapping("/all")
    public Result<List<LabRoom>> all() { return Result.ok(roomService.all()); }

    /** 实验室详情（所有登录用户可查看） */
    @GetMapping("/{id}")
    public Result<LabRoom> detail(@PathVariable Long id) { return Result.ok(roomService.detail(id)); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> create(@RequestBody LabRoom r) {
        Long id = roomService.create(r);
        cache.evictOverview(); cache.evictUsage();
        return Result.ok(id);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN')")
    public Result<?> update(@RequestBody LabRoom r) {
        roomService.update(r);
        cache.evictOverview(); cache.evictUsage();
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> delete(@PathVariable Long id) {
        roomService.delete(id);
        cache.evictOverview(); cache.evictUsage();
        return Result.ok();
    }
}
