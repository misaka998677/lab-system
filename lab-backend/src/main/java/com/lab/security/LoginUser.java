package com.lab.security;

import com.lab.module.system.entity.SysUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class LoginUser implements UserDetails {
    private final SysUser user;
    private final Set<String> roles;
    private final Set<String> perms;

    public LoginUser(SysUser user, Set<String> roles, Set<String> perms) {
        this.user = user; this.roles = roles; this.perms = perms;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> list = roles.stream()
                .map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        perms.forEach(p -> list.add(new SimpleGrantedAuthority(p)));
        return list;
    }
    @Override public String  getPassword()              { return user.getPassword(); }
    @Override public String  getUsername()              { return user.getUsername(); }
    @Override public boolean isAccountNonExpired()      { return true; }
    @Override public boolean isAccountNonLocked()       { return true; }
    @Override public boolean isCredentialsNonExpired()  { return true; }
    @Override public boolean isEnabled()                { return user.getStatus() != null && user.getStatus() == 1; }
}
