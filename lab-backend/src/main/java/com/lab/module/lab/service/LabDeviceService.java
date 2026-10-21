package com.lab.module.lab.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lab.common.BizException;
import com.lab.common.PageResult;
import com.lab.common.SqlLikeUtil;
import com.lab.module.lab.entity.LabDevice;
import com.lab.module.lab.mapper.LabDeviceMapper;
import com.lab.module.lab.mapper.LabRoomMapper;
import com.lab.security.DataScopeUtil;
import com.lab.security.SecurityUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LabDeviceService {
    private final LabDeviceMapper deviceMapper;
    private final LabRoomMapper roomMapper;
    public LabDeviceService(LabDeviceMapper m, LabRoomMapper rm) {
        this.deviceMapper = m; this.roomMapper = rm;
    }

    public PageResult<LabDevice> page(int pageNum, int pageSize, String keyword,
                                      Long labId, String category, Integer status) {
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();
        Long filteredLabId = scopeFilter(labId, scope);
        PageHelper.startPage(pageNum, pageSize);
        return PageResult.of(new PageInfo<>(
            deviceMapper.page(SqlLikeUtil.escape(keyword), filteredLabId, category, status, scope)));
    }

    public List<LabDevice> findByLab(Long labId) { return deviceMapper.findByLab(labId); }
    public LabDevice detail(Long id)             { return deviceMapper.findById(id); }

    public Long create(LabDevice d) {
        if (d.getStatus() == null) d.setStatus(1);
        validateLabOwnership(d.getLabId());
        deviceMapper.insert(d); return d.getId();
    }

    public void update(LabDevice d) {
        if (DataScopeUtil.isLabAdmin()) {
            LabDevice existing = deviceMapper.findById(d.getId());
            if (existing == null) throw new BizException("设备不存在");
            validateLabOwnership(existing.getLabId());
        }
        deviceMapper.update(d);
    }

    public void delete(Long id) {
        if (DataScopeUtil.isLabAdmin()) {
            LabDevice existing = deviceMapper.findById(id);
            if (existing == null) throw new BizException("设备不存在");
            validateLabOwnership(existing.getLabId());
        }
        deviceMapper.deleteById(id);
    }

    public void status(Long id, Integer s) {
        if (DataScopeUtil.isLabAdmin()) {
            LabDevice existing = deviceMapper.findById(id);
            if (existing == null) throw new BizException("设备不存在");
            validateLabOwnership(existing.getLabId());
        }
        deviceMapper.updateStatus(id, s);
    }

    private Long scopeFilter(Long labId, List<Long> scope) {
        if (scope == null || scope.isEmpty()) return labId;
        if (labId == null) return null;
        if (!scope.contains(labId)) {
            return null;
        }
        return labId;
    }

    private void validateLabOwnership(Long labId) {
        if (labId == null) throw new BizException(400, "设备必须归属某个实验室");
        List<Long> myLabs = DataScopeUtil.getLabIdsForLabAdmin();
        if (myLabs == null) return;
        if (myLabs.isEmpty() || !myLabs.contains(labId)) {
            throw new BizException(403, "无权操作其他实验室的设备");
        }
    }

    public List<LabDevice> listAll() {
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();
        return deviceMapper.findAll(scope);
    }
}
