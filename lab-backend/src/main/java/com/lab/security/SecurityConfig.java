package com.lab.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) { this.jwtAuthFilter = jwtAuthFilter; }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(c -> {})
            .sessionManagement(s -> s
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                // 明确禁用 session fixation 保护（JWT 场景下无需 cookie-session）
                .sessionFixation(fix -> fix.none())
            )
            .headers(h -> h
                // X-Frame-Options: DENY —— 防止点击劫持
                .frameOptions(f -> f.deny())
                // X-Content-Type-Options: nosniff —— 防止浏览器嗅探 MIME
                .contentTypeOptions(c -> {})
                // Strict-Transport-Security: max-age=... —— 强制 HTTPS（HTTPS 部署时自动生效）
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            )
            .authorizeHttpRequests(a -> a
                .requestMatchers("/auth/login", "/auth/register", "/auth/captcha", "/auth/logout", "/system/dept/all", "/ws/**").permitAll()
                .requestMatchers("/error", "/favicon.ico").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, rsp, ex) -> writeJson(rsp, 401, "未登录或登录已过期"))
                .accessDeniedHandler((req, rsp, ex) -> writeJson(rsp, 403, "无访问权限"))
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeJson(HttpServletResponse rsp, int code, String msg) throws java.io.IOException {
        rsp.setStatus(code);
        rsp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        rsp.setCharacterEncoding("UTF-8");
        rsp.getWriter().write(new ObjectMapper().writeValueAsString(Result.fail(code, msg)));
    }
}
