package com.lab.module.reserve.service;

import cn.hutool.core.util.IdUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lab.common.BizException;
import com.lab.common.ErrorCode;
import com.lab.common.PageResult;
import com.lab.common.SqlLikeUtil;
import com.lab.module.reserve.entity.Reservation;
import com.lab.module.reserve.mapper.ReservationMapper;
import com.lab.security.DataScopeUtil;
import com.lab.security.SecurityUtil;
import com.lab.websocket.StatPushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationMapper reservationMapper;
    private final StatPushService statPushService;

    @Autowired
    public ReservationService(ReservationMapper m, @Lazy StatPushService sps) {
        this.reservationMapper = m;
        this.statPushService = sps;
    }

    public PageResult<Reservation> page(int pageNum, int pageSize,
                                        String keyword, Long userId, Long labId, Integer status,
                                        LocalDateTime start, LocalDateTime end) {
        Long filteredUserId = userId;
        Long filteredLabId = labId;
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();

        if (DataScopeUtil.isAdmin()) {
            // 管理员：无过滤
        } else if (DataScopeUtil.isLabAdmin() || DataScopeUtil.isTeacher()) {
            // 实验室管理员/教师：按 scope 过滤
            filteredUserId = null;
            filteredLabId = (labId != null && (scope == null || scope.isEmpty() || scope.contains(labId))) ? labId : null;
        } else {
            // 学生：只能看自己的预约
            filteredUserId = SecurityUtil.currentUserId();
        }

        PageHelper.startPage(pageNum, pageSize);
        return PageResult.of(new PageInfo<>(
            reservationMapper.page(SqlLikeUtil.escape(keyword), filteredUserId, filteredLabId, status, start, end, scope)));
    }

    public PageResult<Reservation> checkRecords(int pageNum, int pageSize) {
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();
        Long userId = null;

        if (!DataScopeUtil.isAdmin() && !DataScopeUtil.isLabAdmin() && !DataScopeUtil.isTeacher()) {
            userId = SecurityUtil.currentUserId();
        }

        PageHelper.startPage(pageNum, pageSize);
        return PageResult.of(new PageInfo<>(reservationMapper.checkRecords(userId, scope)));
    }

    /**
     * 获取可访问的预约记录
     * <ul>
     *   <li>管理员：可访问任意预约</li>
     *   <li>实验室管理员/教师：可访问其管理范围内的预约</li>
     *   <li>其他：只能访问自己的预约</li>
     * </ul>
     */
    public Reservation detail(Long id) {
        Reservation r = reservationMapper.findById(id);
        if (r == null) return null;
        validateLabAccess(r.getLabId(), "查看");
        requireOwner(r);
        return r;
    }

    /**
     * 校验实验室访问权限
     */
    private void validateLabAccess(Long labId, String action) {
        if (DataScopeUtil.isAdmin()) return;
        if (DataScopeUtil.isLabAdmin() || DataScopeUtil.isTeacher()) {
            List<Long> myLabs = DataScopeUtil.getLabIdsForTeacher();
            if (labId == null || !myLabs.contains(labId)) {
                throw ErrorCode.FORBIDDEN.ex("无权" + action + "其他实验室的预约");
            }
        }
    }

    /** 校验是否为预约所有者 */
    private void requireOwner(Reservation r) {
        Long uid = SecurityUtil.currentUserId();
        if (uid == null || !uid.equals(r.getUserId())) {
            throw ErrorCode.RESERVATION_NOT_YOURS.ex();
        }
    }

    @Transactional
    public Long apply(Reservation r) {
        Long uid = SecurityUtil.currentUserId();
        if (uid == null) throw ErrorCode.UNAUTHORIZED.ex();
        if (r.getStartTime() == null || r.getEndTime() == null
                || !r.getEndTime().isAfter(r.getStartTime())) {
            throw ErrorCode.RESERVATION_TIME_ILLEGAL.ex();
        }
        if (reservationMapper.countConflict(r.getLabId(), r.getStartTime(), r.getEndTime(), null) > 0) {
            throw ErrorCode.RESERVATION_TIME_CONFLICT.ex();
        }
        r.setUserId(uid);
        r.setStatus(0);
        r.setReserveNo("R" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + IdUtil.getSnowflakeNextId() % 1000);
        reservationMapper.insert(r);
        statPushService.pushOverviewUpdate();
        return r.getId();
    }

    @Transactional
    public void update(Reservation r) {
        Reservation old = reservationMapper.findById(r.getId());
        if (old == null) throw ErrorCode.RESERVATION_NOT_FOUND.ex();
        validateLabAccess(old.getLabId(), "修改");
        requireOwner(old);
        if (old.getStatus() != 0) throw ErrorCode.RESERVATION_NOT_PENDING.ex();
        if (r.getStartTime() != null && r.getEndTime() != null
                && reservationMapper.countConflict(
                        r.getLabId() != null ? r.getLabId() : old.getLabId(),
                        r.getStartTime(), r.getEndTime(), r.getId()) > 0) {
            throw ErrorCode.RESERVATION_TIME_CONFLICT.ex("修改后时段冲突");
        }
        reservationMapper.update(r);
    }

    public void cancel(Long id) {
        Reservation r = reservationMapper.findById(id);
        if (r == null) throw ErrorCode.RESERVATION_NOT_FOUND.ex();
        requireOwner(r);
        if (r.getStatus() != 0 && r.getStatus() != 1) {
            throw ErrorCode.STATUS_ILLEGAL.ex("仅待审核/已通过的预约可取消");
        }
        reservationMapper.updateStatus(id, 5, SecurityUtil.currentUserId(), "用户取消");
        statPushService.pushOverviewUpdate();
    }

    @Transactional
    public void audit(Long id, boolean pass, String note) {
        Reservation r = reservationMapper.findById(id);
        if (r == null) throw ErrorCode.RESERVATION_NOT_FOUND.ex();
        if (r.getStatus() != 0) throw ErrorCode.RESERVATION_NOT_PENDING.ex();

        // 只有管理员、实验室管理员、教师可以审核
        if (DataScopeUtil.isLabAdmin() || DataScopeUtil.isTeacher()) {
            List<Long> myLabs = DataScopeUtil.getLabIdsForTeacher();
            if (r.getLabId() == null || !myLabs.contains(r.getLabId())) {
                throw ErrorCode.FORBIDDEN.ex("无权审核其他实验室的预约");
            }
        } else if (!DataScopeUtil.isAdmin()) {
            throw ErrorCode.FORBIDDEN.ex();
        }

        reservationMapper.updateStatus(id, pass ? 1 : 2, SecurityUtil.currentUserId(), note);
        statPushService.pushOverviewUpdate();
    }

    public void checkIn(Long id) {
        Reservation r = reservationMapper.findById(id);
        if (r == null) throw ErrorCode.RESERVATION_NOT_FOUND.ex();
        requireOwner(r);
        if (r.getStatus() != 1) throw ErrorCode.RESERVATION_NOT_APPROVED.ex("仅已通过的预约可签到");

        // 验证当前时间是否在预约时间段内
        LocalDateTime now = LocalDateTime.now();
        if (!isInReservationTime(r, now)) {
            throw ErrorCode.RESERVATION_TIME_ILLEGAL.ex("当前不在预约时间段内，无法签到");
        }

        reservationMapper.checkIn(id, now);
        statPushService.pushOverviewUpdate();
    }

    public void checkOut(Long id) {
        Reservation r = reservationMapper.findById(id);
        if (r == null) throw ErrorCode.RESERVATION_NOT_FOUND.ex();
        requireOwner(r);
        if (r.getStatus() != 3) throw ErrorCode.RESERVATION_ALREADY_CHECKED_IN.ex("仅已签到的预约可签退");

        // 验证当前时间是否在预约时间段内
        LocalDateTime now = LocalDateTime.now();
        if (!isInReservationTime(r, now)) {
            throw ErrorCode.RESERVATION_TIME_ILLEGAL.ex("当前不在预约时间段内，无法签退");
        }

        reservationMapper.checkOut(id, now);
        statPushService.pushOverviewUpdate();
    }

    /**
     * 判断当前时间是否在预约时间段内
     * 允许提前15分钟签到，允许延后30分钟签退（给用户一定的缓冲时间）
     */
    private boolean isInReservationTime(Reservation r, LocalDateTime now) {
        if (r.getStartTime() == null || r.getEndTime() == null) return false;

        // 签入允许提前15分钟
        LocalDateTime effectiveStart = r.getStartTime().minusMinutes(15);
        // 签退允许延后30分钟
        LocalDateTime effectiveEnd = r.getEndTime().plusMinutes(30);

        return !now.isBefore(effectiveStart) && !now.isAfter(effectiveEnd);
    }
}
