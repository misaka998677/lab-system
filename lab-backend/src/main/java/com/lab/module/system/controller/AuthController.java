package com.lab.module.system.controller;

import com.lab.common.Result;
import com.lab.module.system.dto.LoginDTO;
import com.lab.module.system.dto.RegisterDTO;
import com.lab.module.system.entity.SysMenu;
import com.lab.module.system.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    public AuthController(AuthService a) { this.authService = a; }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto.getUsername(), dto.getPassword()));
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterDTO dto) {
        return Result.ok(authService.register(dto));
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info() { return Result.ok(authService.info()); }

    @GetMapping("/menus")
    public Result<List<SysMenu>> menus() { return Result.ok(authService.menus()); }

    @PostMapping("/logout")
    public Result<?> logout() { return Result.ok(); /* JWT 无状态，前端清 token 即可 */ }
}
