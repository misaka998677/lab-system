package com.lab.security;

import com.lab.module.system.entity.SysUser;
import com.lab.module.system.mapper.SysUserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;

    public UserDetailsServiceImpl(SysUserMapper userMapper) { this.userMapper = userMapper; }

    @Override
    public UserDetails loadUserByUsername(String username) {
        SysUser u = userMapper.findByUsername(username);
        if (u == null) throw new UsernameNotFoundException("用户不存在");
        Set<String> roles = new HashSet<>(userMapper.findRoleCodes(u.getId()));
        List<String> perms = userMapper.findPermsByUserId(u.getId());
        return new LoginUser(u, roles, new HashSet<>(perms));
    }
}
