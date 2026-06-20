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
import com.lab.module.system.entity.SysLog;
import com.lab.module.system.mapper.SysUserMapper;
import com.lab.module.system.service.SysLogService;
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
    private final SysLogService sysLogService;
    private final SysUserMapper userMapper;

    @Autowired
    public ReservationService(ReservationMapper m, @Lazy StatPushService sps,
                              @Lazy SysLogService sls, SysUserMapper um) {
        this.reservationMapper = m;
        this.statPushService = sps;
        this.sysLogService = sls;
        this.userMapper = um;
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
        // 预约开始时间不能早于当前时间
        if (r.getStartTime().isBefore(LocalDateTime.now())) {
            throw ErrorCode.RESERVATION_TIME_ILLEGAL.ex("预约开始时间不能早于当前时间");
        }
        // 实验室权限校验：LABADMIN/TEACHER 只能预约自己有权限的实验室
        validateLabAccess(r.getLabId(), "预约");
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
        // 校验权限：若改了实验室，用新 labId 校验；未改则用旧的
        Long targetLabId = r.getLabId() != null ? r.getLabId() : old.getLabId();
        validateLabAccess(targetLabId, "修改");
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

    public void cancel(Long id, String reason) {
        Reservation r = reservationMapper.findById(id);
        if (r == null) throw ErrorCode.RESERVATION_NOT_FOUND.ex();
        requireOwner(r);
        // 已签到时必须填写取消原因
        if (r.getStatus() == 3) {
            if (reason == null || reason.trim().isEmpty()) {
                throw ErrorCode.STATUS_ILLEGAL.ex("已签到的预约取消时必须填写原因");
            }
            reservationMapper.updateStatus(id, 5, SecurityUtil.currentUserId(), "用户取消(已签到)：" + reason);
        } else if (r.getStatus() == 0 || r.getStatus() == 1) {
            reservationMapper.updateStatus(id, 5, SecurityUtil.currentUserId(), reason != null ? "用户取消：" + reason : "用户取消");
        } else {
            throw ErrorCode.STATUS_ILLEGAL.ex("当前状态不允许取消");
        }
        statPushService.pushOverviewUpdate();

        // 记录操作日志
        SysLog log = new SysLog();
        log.setUserId(SecurityUtil.currentUserId());
        log.setUsername(SecurityUtil.current() != null ? SecurityUtil.current().getUsername() : null);
        log.setModule("预约管理");
        log.setAction("取消预约");
        log.setMethod("ReservationService.cancel");
        log.setParams("id=" + id + ", reason=" + reason);
        log.setStatus(1);
        sysLogService.asyncSave(log);
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
            // 教师审核时，检查预约学生是否属于同一学院
            if (DataScopeUtil.isTeacher()) {
                Long teacherDeptId = DataScopeUtil.getCurrentDeptId();
                Long studentDeptId = userMapper.findDeptIdByUserId(r.getUserId());
                if (studentDeptId == null || !studentDeptId.equals(teacherDeptId)) {
                    throw ErrorCode.FORBIDDEN.ex("无权审核其他学院学生的预约");
                }
            }
        } else if (!DataScopeUtil.isAdmin()) {
            throw ErrorCode.FORBIDDEN.ex();
        }

        reservationMapper.updateStatus(id, pass ? 1 : 2, SecurityUtil.currentUserId(), note);
        statPushService.pushOverviewUpdate();

        // 记录操作日志
        SysLog log = new SysLog();
        log.setUserId(SecurityUtil.currentUserId());
        log.setUsername(SecurityUtil.current() != null ? SecurityUtil.current().getUsername() : null);
        log.setModule("预约审核");
        log.setAction(pass ? "通过" : "驳回");
        log.setMethod("ReservationService.audit");
        log.setParams("id=" + id + ", pass=" + pass + ", note=" + note);
        log.setStatus(1);
        sysLogService.asyncSave(log);
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
     * 判断当前时间是否在预约时间段内（签到/签退有效窗口）
     * 与 ReservationTimeoutService 保持一致：签入/签退均使用 30 分钟超时窗口
     * - 签入有效窗口：预约开始时间前30分钟 ~ 预约开始时间后30分钟
     * - 签退有效窗口：预约结束时间前30分钟 ~ 预约结束时间后30分钟
     */
    private boolean isInReservationTime(Reservation r, LocalDateTime now) {
        if (r.getStartTime() == null || r.getEndTime() == null) return false;

        // 签入：预约开始前后30分钟内可签入（与超时任务一致）
        LocalDateTime effectiveStart = r.getStartTime().plusMinutes(30);
        if (now.isAfter(effectiveStart)) return false;

        // 签退：预约结束前后30分钟内可签退
        LocalDateTime effectiveEnd = r.getEndTime().plusMinutes(30);
        return !now.isAfter(effectiveEnd);
    }
}
