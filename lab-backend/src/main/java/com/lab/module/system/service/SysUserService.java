package com.lab.module.system.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lab.common.BizException;
import com.lab.common.PageResult;
import com.lab.common.SqlLikeUtil;
import com.lab.module.system.entity.SysUser;
import com.lab.module.system.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysUserService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public SysUserService(SysUserMapper m, PasswordEncoder p) { this.userMapper = m; this.passwordEncoder = p; }

    public PageResult<SysUser> page(int pageNum, int pageSize, String keyword, Long deptId, Integer status) {
        PageHelper.startPage(pageNum, pageSize);
        List<SysUser> list = userMapper.page(SqlLikeUtil.escape(keyword), deptId, status);
        list.forEach(u -> u.setPassword(null));
        return PageResult.of(new PageInfo<>(list));
    }

    public SysUser detail(Long id) {
        SysUser u = userMapper.findById(id);
        if (u == null) throw new BizException("用户不存在");
        u.setPassword(null);
        u.setRoleIds(userMapper.findRoleIdsByUserId(id));
        return u;
    }

    @Transactional
    public Long create(SysUser u, List<Long> roleIds) {
        if (userMapper.findByUsername(u.getUsername()) != null) throw new BizException("登录名已存在");
        u.setPassword(passwordEncoder.encode(u.getPassword() == null ? "123456" : u.getPassword()));
        if (u.getStatus() == null) u.setStatus(1);
        userMapper.insert(u);
        if (roleIds != null && !roleIds.isEmpty()) userMapper.insertUserRoles(u.getId(), roleIds);
        return u.getId();
    }

    @Transactional
    public void update(SysUser u, List<Long> roleIds) {
        userMapper.update(u);
        userMapper.deleteUserRoles(u.getId());
        if (roleIds != null && !roleIds.isEmpty()) userMapper.insertUserRoles(u.getId(), roleIds);
    }

    public void delete(Long id) { userMapper.deleteById(id); }

    public void changeStatus(Long id, Integer status) { userMapper.updateStatus(id, status); }

    public void resetPassword(Long id, String pwd) {
        userMapper.updatePassword(id, passwordEncoder.encode(pwd == null ? "123456" : pwd));
    }

    public List<SysUser> listAll() {
        List<SysUser> list = userMapper.findAll();
        list.forEach(u -> u.setPassword(null));
        return list;
    }
}
