package com.lab.module.system.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lab.common.PageResult;
import com.lab.common.SqlLikeUtil;
import com.lab.module.system.entity.SysRole;
import com.lab.module.system.mapper.SysRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    public SysRoleService(SysRoleMapper m) { this.roleMapper = m; }

    public PageResult<SysRole> page(int pageNum, int pageSize, String keyword) {
        PageHelper.startPage(pageNum, pageSize);
        return PageResult.of(new PageInfo<>(roleMapper.page(SqlLikeUtil.escape(keyword))));
    }

    public List<SysRole> all() { return roleMapper.all(); }

    public SysRole detail(Long id) {
        SysRole r = roleMapper.findById(id);
        if (r != null) r.setMenuIds(roleMapper.findMenuIds(id));
        return r;
    }

    @Transactional
    public Long create(SysRole r, List<Long> menuIds) {
        if (r.getStatus() == null) r.setStatus(1);
        roleMapper.insert(r);
        if (menuIds != null && !menuIds.isEmpty()) roleMapper.insertRoleMenus(r.getId(), menuIds);
        return r.getId();
    }

    @Transactional
    public void update(SysRole r, List<Long> menuIds) {
        roleMapper.update(r);
        roleMapper.deleteRoleMenus(r.getId());
        if (menuIds != null && !menuIds.isEmpty()) roleMapper.insertRoleMenus(r.getId(), menuIds);
    }

    public void delete(Long id) { roleMapper.deleteById(id); }
}
