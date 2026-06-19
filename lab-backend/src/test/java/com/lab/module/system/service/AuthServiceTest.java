package com.lab.module.system.service;

import com.lab.common.BizException;
import com.lab.module.system.dto.RegisterDTO;
import com.lab.module.system.entity.SysUser;
import com.lab.module.system.mapper.SysMenuMapper;
import com.lab.module.system.mapper.SysRoleMapper;
import com.lab.module.system.mapper.SysUserMapper;
import com.lab.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private SysUserMapper userMapper;
    private SysMenuMapper menuMapper;
    private SysRoleMapper roleMapper;
    private SysUserService userService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        jwtService = mock(JwtService.class);
        userMapper = mock(SysUserMapper.class);
        menuMapper = mock(SysMenuMapper.class);
        roleMapper = mock(SysRoleMapper.class);
        userService = mock(SysUserService.class);
        authService = new AuthService(authenticationManager, jwtService, userMapper, menuMapper, roleMapper, userService);
    }

    @Test
    void studentRegisterFindsStudentRoleByCodeAndCreatesEnabledUser() {
        when(roleMapper.findIdByRoleCode("ROLE_STUDENT")).thenReturn(40L);
        when(userService.create(any(SysUser.class), any())).thenReturn(100L);
        RegisterDTO dto = dto(" student_new ", "123456", "123456", " 新学生 ", "student");
        dto.setPhone(" 13800000000 ");
        dto.setEmail(" student_new@example.com ");

        String message = authService.register(dto);

        assertEquals("注册成功，请使用账号密码登录。", message);
        verify(roleMapper).findIdByRoleCode("ROLE_STUDENT");
        verify(userMapper).findByUsername("student_new");
        verify(userService).create(argThat(u ->
                "student_new".equals(u.getUsername())
                        && "123456".equals(u.getPassword())
                        && "新学生".equals(u.getRealName())
                        && "13800000000".equals(u.getPhone())
                        && "student_new@example.com".equals(u.getEmail())
                        && Integer.valueOf(0).equals(u.getGender())
                        && Integer.valueOf(1).equals(u.getStatus())
        ), eq(List.of(40L)));
    }

    @Test
    void teacherRegisterTrimsRoleTypeFindsTeacherRoleByCodeAndCreatesPendingUser() {
        when(roleMapper.findIdByRoleCode("ROLE_TEACHER")).thenReturn(30L);
        when(userService.create(any(SysUser.class), any())).thenReturn(101L);
        RegisterDTO dto = dto(" teacher_new ", "123456", "123456", " 新教师 ", " Teacher ");

        String message = authService.register(dto);

        assertEquals("教师账号已提交，请等待管理员审核启用。", message);
        verify(roleMapper).findIdByRoleCode("ROLE_TEACHER");
        verify(userService).create(argThat(u ->
                "teacher_new".equals(u.getUsername())
                        && "新教师".equals(u.getRealName())
                        && Integer.valueOf(0).equals(u.getStatus())
        ), eq(List.of(30L)));
    }

    @Test
    void registerRejectsDuplicateUsernameWithBusinessCodeBeforeCreate() {
        SysUser existing = new SysUser();
        existing.setId(1L);
        when(userMapper.findByUsername("student_new")).thenReturn(existing);
        RegisterDTO dto = dto(" student_new ", "123456", "123456", "新学生", "student");

        BizException ex = assertThrows(BizException.class, () -> authService.register(dto));

        assertEquals(400, ex.getCode());
        assertEquals("登录名已存在", ex.getMessage());
        verify(userService, never()).create(any(), any());
        verify(roleMapper, never()).findIdByRoleCode(any());
    }

    @Test
    void registerRejectsPasswordConfirmationMismatch() {
        RegisterDTO dto = dto("student_new", "123456", "654321", "新学生", "student");

        BizException ex = assertThrows(BizException.class, () -> authService.register(dto));

        assertEquals(400, ex.getCode());
        assertEquals("两次输入的密码不一致", ex.getMessage());
        verify(userService, never()).create(any(), any());
    }

    @Test
    void registerRejectsBlankUsernamePasswordOrRealName() {
        assertBlankRequired(dto("   ", "123456", "123456", "新学生", "student"));
        assertBlankRequired(dto("student_new", "   ", "   ", "新学生", "student"));
        assertBlankRequired(dto("student_new", "123456", "123456", "   ", "student"));
        verify(userService, never()).create(any(), any());
    }

    @Test
    void registerRejectsMissingRoleConfiguration() {
        when(roleMapper.findIdByRoleCode("ROLE_STUDENT")).thenReturn(null);
        RegisterDTO dto = dto("student_new", "123456", "123456", "新学生", "student");

        BizException ex = assertThrows(BizException.class, () -> authService.register(dto));

        assertEquals(500, ex.getCode());
        assertEquals("角色配置缺失", ex.getMessage());
        verify(userService, never()).create(any(), any());
    }

    @Test
    void registerRejectsUnsupportedRoleType() {
        RegisterDTO dto = dto("admin_new", "123456", "123456", "管理员", "admin");

        BizException ex = assertThrows(BizException.class, () -> authService.register(dto));

        assertEquals(400, ex.getCode());
        assertEquals("仅支持学生或教师注册", ex.getMessage());
        verify(userService, never()).create(any(), any());
    }

    @Test
    void loginReportsDisabledAccountWithoutChangingBadCredentialsMessage() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("disabled"));

        BizException ex = assertThrows(BizException.class, () -> authService.login("teacher_new", "123456"));

        assertEquals(401, ex.getCode());
        assertEquals("账号待审核或已禁用，请联系管理员", ex.getMessage());
    }

    @Test
    void loginReportsBadCredentialsAsAccountOrPasswordError() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        BizException ex = assertThrows(BizException.class, () -> authService.login("student_new", "wrong"));

        assertEquals(401, ex.getCode());
        assertEquals("账号或密码错误", ex.getMessage());
    }

    private void assertBlankRequired(RegisterDTO dto) {
        BizException ex = assertThrows(BizException.class, () -> authService.register(dto));
        assertEquals(400, ex.getCode());
        assertEquals("用户名、密码和真实姓名不能为空", ex.getMessage());
    }

    private RegisterDTO dto(String username, String password, String confirmPassword, String realName, String roleType) {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setConfirmPassword(confirmPassword);
        dto.setRealName(realName);
        dto.setPhone("13800000000");
        dto.setEmail(username == null ? null : username.trim() + "@example.com");
        dto.setRoleType(roleType);
        dto.setDeptId(1L);
        return dto;
    }
}
