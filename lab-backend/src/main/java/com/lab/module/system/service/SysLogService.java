package com.lab.module.system.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lab.common.PageResult;
import com.lab.common.SqlLikeUtil;
import com.lab.module.system.entity.SysLog;
import com.lab.module.system.mapper.SysLogMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysLogService {
    private final SysLogMapper logMapper;
    public SysLogService(SysLogMapper m) { this.logMapper = m; }

    @Async
    public void asyncSave(SysLog l) { logMapper.insert(l); }

    public PageResult<SysLog> page(int pageNum, int pageSize, String username,
                                   String module, Integer status,
                                   java.time.LocalDateTime start, java.time.LocalDateTime end) {
        PageHelper.startPage(pageNum, pageSize);
        return PageResult.of(new PageInfo<>(
            logMapper.page(SqlLikeUtil.escape(username), module, status, start, end)));
    }

    public void clear() { logMapper.clear(); }

    public List<SysLog> all() { return logMapper.selectAll(); }
}
