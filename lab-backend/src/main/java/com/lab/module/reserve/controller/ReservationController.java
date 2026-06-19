package com.lab.module.reserve.controller;

import com.lab.common.PageParam;
import com.lab.common.PageResult;
import com.lab.common.Result;
import com.lab.config.StatCacheInvalidator;
import com.lab.module.reserve.entity.Reservation;
import com.lab.module.reserve.service.ReservationService;
import com.lab.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/reserve")
public class ReservationController {

    private final ReservationService reservationService;
    private final StatCacheInvalidator cache;

    @Autowired
    public ReservationController(ReservationService s, StatCacheInvalidator c) {
        this.reservationService = s;
        this.cache = c;
    }

    /** 全部预约（管理员/老师审核用） */
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN','TEACHER')")
    public Result<PageResult<Reservation>> page(@RequestParam(defaultValue = "1")  int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long labId,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        int[] p = PageParam.clamp(pageNum, pageSize);
        return Result.ok(reservationService.page(p[0], p[1], keyword, null, labId, status, start, end));
    }

    /** 我的预约 */
    @GetMapping("/mine")
    public Result<PageResult<Reservation>> mine(@RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize,
                                                @RequestParam(required = false) Integer status) {
        int[] p = PageParam.clamp(pageNum, pageSize);
        return Result.ok(reservationService.page(p[0], p[1], null,
                SecurityUtil.currentUserId(), null, status, null, null));
    }

    /** 签到记录：所有登录用户查看自己的签到记录；管理员查看全部 */
    @GetMapping("/check-records")
    public Result<PageResult<Reservation>> checkRecords(@RequestParam(defaultValue = "1") int pageNum,
                                                        @RequestParam(defaultValue = "10") int pageSize) {
        int[] p = PageParam.clamp(pageNum, pageSize);
        return Result.ok(reservationService.checkRecords(p[0], p[1]));
    }

    @GetMapping("/{id}")
    public Result<Reservation> detail(@PathVariable Long id) { return Result.ok(reservationService.detail(id)); }

    @PostMapping
    public Result<Long> apply(@RequestBody Reservation r) {
        Long id = reservationService.apply(r);
        cache.evictOverview(); cache.evictUsage();
        return Result.ok(id);
    }

    @PutMapping
    public Result<?> update(@RequestBody Reservation r) {
        reservationService.update(r);
        cache.evictOverview(); cache.evictUsage();
        return Result.ok();
    }

    @PutMapping("/{id}/cancel")
    public Result<?> cancel(@PathVariable Long id) {
        reservationService.cancel(id);
        cache.evictOverview(); cache.evictUsage();
        return Result.ok();
    }

    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('ADMIN','LABADMIN','TEACHER')")
    public Result<?> audit(@PathVariable Long id,
                           @RequestParam boolean pass,
                           @RequestParam(required = false) String note) {
        reservationService.audit(id, pass, note);
        cache.evictOverview(); cache.evictUsage();
        return Result.ok();
    }

    @PutMapping("/{id}/check-in")
    public Result<?> checkIn(@PathVariable Long id) {
        reservationService.checkIn(id);
        cache.evictOverview(); cache.evictUsage();
        return Result.ok();
    }

    @PutMapping("/{id}/check-out")
    public Result<?> checkOut(@PathVariable Long id) {
        reservationService.checkOut(id);
        cache.evictOverview(); cache.evictUsage();
        return Result.ok();
    }
}
