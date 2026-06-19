package com.lab.security;

import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {
    private SecurityUtil() {}

    public static LoginUser current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser u) return u;
        return null;
    }

    public static Long currentUserId() {
        LoginUser u = current();
        return u == null ? null : u.getUser().getId();
    }
}
