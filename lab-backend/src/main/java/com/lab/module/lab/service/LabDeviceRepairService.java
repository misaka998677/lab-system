package com.lab.module.lab.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lab.common.BizException;
import com.lab.common.PageResult;
import com.lab.common.SqlLikeUtil;
import com.lab.module.lab.entity.LabDevice;
import com.lab.module.lab.entity.LabDeviceRepair;
import com.lab.module.lab.mapper.LabDeviceMapper;
import com.lab.module.lab.mapper.LabDeviceRepairMapper;
import com.lab.security.DataScopeUtil;
import com.lab.security.LoginUser;
import com.lab.security.SecurityUtil;
import com.lab.websocket.StatPushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LabDeviceRepairService {
    private final LabDeviceRepairMapper repairMapper;
    private final LabDeviceMapper       deviceMapper;
    private final StatPushService       statPushService;

    @Autowired
    public LabDeviceRepairService(LabDeviceRepairMapper r, LabDeviceMapper d, @Lazy StatPushService sps) {
        this.repairMapper = r; this.deviceMapper = d; this.statPushService = sps;
    }

    public PageResult<LabDeviceRepair> page(int pageNum, int pageSize,
                                            String keyword, Integer status, Long deviceId) {
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();
        Long reporterId;
        if (DataScopeUtil.isAdmin()) {
            reporterId = null;
        } else if (DataScopeUtil.isLabAdmin()) {
            reporterId = null;
        } else {
            reporterId = SecurityUtil.currentUserId();
        }
        PageHelper.startPage(pageNum, pageSize);
        return PageResult.of(new PageInfo<>(
            repairMapper.page(SqlLikeUtil.escape(keyword), status, deviceId, reporterId, scope)));
    }

    /**
     * 当前用户查看自己提交的报修记录。
     * 所有登录用户均可调用（学生/教师/LABADMIN/ADMIN 都可以看到自己提交的）。
     * 按 reporter_id = 当前用户 ID 过滤。
     */
    public PageResult<LabDeviceRepair> mine(int pageNum, int pageSize,
                                            String keyword, Integer status) {
        Long currentUserId = SecurityUtil.currentUserId();
        if (currentUserId == null) {
            throw new BizException("未登录");
        }
        PageHelper.startPage(pageNum, pageSize);
        return PageResult.of(new PageInfo<>(
            repairMapper.page(SqlLikeUtil.escape(keyword), status, null, currentUserId, null)));
    }

    @Transactional
    public Long report(LabDeviceRepair r) {
        Long uid = SecurityUtil.currentUserId();
        if (uid == null) throw new BizException("未登录");

        // ==== 设备状态校验 ====
        LabDevice device = deviceMapper.findById(r.getDeviceId());
        if (device == null) throw new BizException("设备不存在");
        if (device.getStatus() != null && device.getStatus() == 3) {
            throw new BizException("该设备已报废，无法报修");
        }
        if (device.getStatus() != null && device.getStatus() == 2) {
            throw new BizException("该设备正在维修中，请勿重复报修");
        }
        // 检查是否已有未完成的维修单（status 为 0 待处理 或 1 处理中）
        int pendingCount = repairMapper.countByDeviceIdAndStatus(r.getDeviceId());
        if (pendingCount > 0) {
            throw new BizException("该设备已有 " + pendingCount + " 条未完成的维修记录，请先处理完成后再报修");
        }

        r.setReporterId(uid);
        r.setStatus(0);
        r.setReportTime(LocalDateTime.now());
        repairMapper.insert(r);
        deviceMapper.updateStatus(r.getDeviceId(), 2);
        statPushService.pushOverviewUpdate();
        return r.getId();
    }

    @Transactional
    public void handle(Long id, Long handlerId, String note, Integer status) {
        LoginUser user = SecurityUtil.current();
        if (user == null) throw new BizException(401, "未登录");
        boolean isAdmin = DataScopeUtil.isAdmin();
        boolean isLabAdmin = DataScopeUtil.isLabAdmin();
        if (!isAdmin && !isLabAdmin) throw new BizException(403, "仅管理员可处理维修单");

        LabDeviceRepair r = repairMapper.findById(id);
        if (r == null) throw new BizException("维修单不存在");

        if (isLabAdmin) {
            LabDevice d = deviceMapper.findById(r.getDeviceId());
            if (d == null) throw new BizException("设备不存在");
            List<Long> myLabs = DataScopeUtil.getLabIdsForLabAdmin();
            if (d.getLabId() == null || !myLabs.contains(d.getLabId())) {
                throw new BizException(403, "无权处理其他实验室的维修单");
            }
        }

        LabDeviceRepair u = new LabDeviceRepair();
        u.setId(id);
        u.setHandlerId(handlerId == null ? user.getUser().getId() : handlerId);
        u.setHandleNote(note);
        u.setStatus(status);
        if (status != null && status == 2) {
            u.setFinishTime(LocalDateTime.now());
            deviceMapper.updateStatus(r.getDeviceId(), 1);
        }
        repairMapper.update(u);
        statPushService.pushOverviewUpdate();
    }

    public void delete(Long id) {
        if (SecurityUtil.current() == null) throw new BizException(401, "未登录");
        boolean isAdmin = DataScopeUtil.isAdmin();
        boolean isLabAdmin = DataScopeUtil.isLabAdmin();
        if (!isAdmin && !isLabAdmin) throw new BizException(403, "仅管理员可删除维修单");

        if (isLabAdmin) {
            LabDeviceRepair r = repairMapper.findById(id);
            if (r == null) throw new BizException("维修单不存在");
            LabDevice d = deviceMapper.findById(r.getDeviceId());
            if (d == null) throw new BizException("设备不存在");
            List<Long> myLabs = DataScopeUtil.getLabIdsForLabAdmin();
            if (d.getLabId() == null || !myLabs.contains(d.getLabId())) {
                throw new BizException(403, "无权删除其他实验室的维修单");
            }
        }

        repairMapper.deleteById(id);
        statPushService.pushOverviewUpdate();
    }

    public List<LabDeviceRepair> listAll() {
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();
        return repairMapper.findAll(scope);
    }
}
