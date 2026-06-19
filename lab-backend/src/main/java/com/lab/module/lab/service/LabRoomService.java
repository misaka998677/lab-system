package com.lab.module.lab.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lab.common.BizException;
import com.lab.common.PageResult;
import com.lab.common.SqlLikeUtil;
import com.lab.module.lab.entity.LabRoom;
import com.lab.module.lab.mapper.LabRoomMapper;
import com.lab.security.DataScopeUtil;
import com.lab.security.SecurityUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LabRoomService {
    private final LabRoomMapper roomMapper;
    public LabRoomService(LabRoomMapper m) { this.roomMapper = m; }

    public PageResult<LabRoom> page(int pageNum, int pageSize, String keyword, Long deptId, Integer status) {
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();
        PageHelper.startPage(pageNum, pageSize);
        return PageResult.of(new PageInfo<>(
            roomMapper.page(SqlLikeUtil.escape(keyword), deptId, status, scope)));
    }

    public List<LabRoom> all() { return roomMapper.all(DataScopeUtil.getLabIdsForTeacher()); }
    public LabRoom detail(Long id) { return roomMapper.findById(id); }

    public Long create(LabRoom r) {
        if (r.getStatus() == null) r.setStatus(1);
        roomMapper.insert(r); return r.getId();
    }

    public void update(LabRoom r) {
        if (DataScopeUtil.isLabAdmin()) {
            LabRoom existing = roomMapper.findById(r.getId());
            if (existing == null) throw new BizException("实验室不存在");
            if (existing.getManagerId() == null || !existing.getManagerId().equals(SecurityUtil.currentUserId())) {
                throw new BizException(403, "无权修改不属于自己的实验室");
            }
        }
        roomMapper.update(r);
    }

    public void delete(Long id) {
        roomMapper.deleteById(id);
    }
}
