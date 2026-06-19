package com.lab.module.system.service;

import com.lab.common.BizException;
import com.lab.module.system.dto.RegisterDTO;
import com.lab.module.system.entity.SysMenu;
import com.lab.module.system.entity.SysUser;
import com.lab.module.system.mapper.SysMenuMapper;
import com.lab.module.system.mapper.SysRoleMapper;
import com.lab.module.system.mapper.SysUserMapper;
import com.lab.security.JwtService;
import com.lab.security.LoginUser;
import com.lab.security.SecurityUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AuthService {

    /** 单账号最大连续失败次数；超过则临时锁定。 */
    private static final int MAX_FAILS = 5;
    /** 锁定时长（秒）。 */
    private static final int LOCK_SECONDS = 300;
    private final Map<String, FailureEntry> failureMap = new ConcurrentHashMap<>();

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserService userService;

    public AuthService(AuthenticationManager am, JwtService j, SysUserMapper um,
                       SysMenuMapper mm, SysRoleMapper rm, SysUserService us) {
        this.authenticationManager = am; this.jwtService = j; this.userMapper = um;
        this.menuMapper = mm; this.roleMapper = rm; this.userService = us;
    }

    public Map<String, Object> login(String username, String password) {
        String key = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        // 1. 先检查是否被临时锁定
        FailureEntry entry = failureMap.get(key);
        if (entry != null && entry.isLocked()) {
            long wait = entry.remainSeconds();
            throw new BizException(400, "账号或密码错误次数过多，请 " + wait + " 秒后再试");
        }
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            LoginUser u = (LoginUser) auth.getPrincipal();
            String token = jwtService.issue(u.getUser().getId(), u.getUsername());
            // 登录成功：清理失败计数
            failureMap.remove(key);
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("username", u.getUsername());
            data.put("realName", u.getUser().getRealName());
            data.put("userId",   u.getUser().getId());
            return data;
        } catch (DisabledException e) {
            throw new BizException(401, "账号待审核或已禁用，请联系管理员");
        } catch (BadCredentialsException e) {
            // 登录失败：累计失败次数
            failureMap.compute(key, (k, old) -> {
                FailureEntry cur = old == null ? new FailureEntry() : old;
                cur.onFail();
                return cur;
            });
            int remain = MAX_FAILS - failureMap.get(key).failCount;
            if (remain <= 0) {
                throw new BizException(400, "尝试次数过多，请 " + LOCK_SECONDS + " 秒后再试");
            }
            throw new BizException(401, "账号或密码错误（还可尝试 " + remain + " 次）");
        }
    }

    public String register(RegisterDTO dto) {
        String username = dto == null ? null : trimToNull(dto.getUsername());
        String password = dto == null ? null : dto.getPassword();
        String realName = dto == null ? null : trimToNull(dto.getRealName());
        if (username == null || isBlank(password) || realName == null) {
            throw new BizException(400, "用户名、密码和真实姓名不能为空");
        }
        if (password.length() < 6) {
            throw new BizException(400, "密码至少 6 位");
        }
        if (!Objects.equals(password, dto.getConfirmPassword())) {
            throw new BizException(400, "两次输入的密码不一致");
        }
        if (userMapper.findByUsername(username) != null) {
            throw new BizException(400, "登录名已存在");
        }

        Long roleId;
        Integer status;
        String message;
        String roleType = dto.getRoleType() == null ? "" : dto.getRoleType().trim().toLowerCase();
        if ("student".equals(roleType)) {
            roleId = findRequiredRoleId("ROLE_STUDENT");
            status = 1;
            message = "注册成功，请使用账号密码登录。";
        } else if ("teacher".equals(roleType)) {
            roleId = findRequiredRoleId("ROLE_TEACHER");
            status = 0;
            message = "教师账号已提交，请等待管理员审核启用。";
        } else {
            throw new BizException(400, "仅支持学生或教师注册");
        }

        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(password);
        u.setRealName(realName);
        u.setPhone(trimToNull(dto.getPhone()));
        u.setEmail(trimToNull(dto.getEmail()));
        u.setGender(0);
        u.setDeptId(dto.getDeptId());
        u.setStatus(status);
        userService.create(u, List.of(roleId));
        return message;
    }

    private Long findRequiredRoleId(String roleCode) {
        Long roleId = roleMapper.findIdByRoleCode(roleCode);
        if (roleId == null) throw new BizException(500, "角色配置缺失");
        return roleId;
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public Map<String, Object> info() {
        LoginUser u = SecurityUtil.current();
        if (u == null) throw new BizException(401, "未登录");
        SysUser full = userMapper.findById(u.getUser().getId());
        Map<String, Object> data = new HashMap<>();
        data.put("user",  full);
        data.put("roles", u.getRoles());
        data.put("perms", u.getPerms());
        return data;
    }

    /** 当前用户的菜单树（前端动态路由） */
    public List<SysMenu> menus() {
        LoginUser u = SecurityUtil.current();
        if (u == null) return Collections.emptyList();
        List<SysMenu> all = menuMapper.findByUserId(u.getUser().getId());
        return buildTree(all, 0L);
    }

    private List<SysMenu> buildTree(List<SysMenu> all, Long parentId) {
        return all.stream()
                .filter(m -> Objects.equals(m.getParentId(), parentId))
                .peek(m -> m.setChildren(buildTree(all, m.getId())))
                .collect(Collectors.toList());
    }

    /** 仅内部使用的失败计数对象，不暴露到外部。 */
    private static final class FailureEntry {
        int failCount;
        long firstFailAt;
        synchronized void onFail() {
            if (failCount == 0 || isExpired()) {
                failCount = 1;
                firstFailAt = System.currentTimeMillis();
            } else {
                failCount += 1;
            }
        }
        synchronized boolean isLocked() {
            return failCount >= MAX_FAILS && !isExpired();
        }
        synchronized long remainSeconds() {
            long elapsed = (System.currentTimeMillis() - firstFailAt) / 1000;
            long remain = LOCK_SECONDS - elapsed;
            return remain > 0 ? remain : 0;
        }
        private boolean isExpired() {
            return (System.currentTimeMillis() - firstFailAt) > TimeUnit.SECONDS.toMillis(LOCK_SECONDS);
        }
    }
}
