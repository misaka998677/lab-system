package com.lab.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 请求防重放拦截器。
 *
 * 前端每个请求携带唯一的 X-Request-Id 头，后端在 60 秒窗口内拒绝相同 ID 的重复提交。
 * 防止：
 * - 恶意重放攻击（截获请求后重复发送）
 * - 前端重复点击导致数据重复写入
 *
 * 注意：此方案仅针对已登录用户有效（基于 userId + requestId 组合键），
 * 未登录请求直接放行（因为无法关联用户身份）。
 */
@Component
public class ReplayGuardInterceptor implements HandlerInterceptor {

    /** requestId → (userId, timestamp) */
    private final Map<String, Entry> seen = new ConcurrentHashMap<>();

    /** 重复提交判定窗口：秒。 */
    private static final int WINDOW_SECONDS = 60;

    private static final String HEADER = "X-Request-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String rid = request.getHeader(HEADER);
        if (rid == null || rid.isBlank()) {
            // 未携带 requestId 的请求，直接放行（前端应始终发送）
            return true;
        }

        // 对于匿名请求（无 session）不做重复检查
        String userId = request.getHeader("X-User-Id");
        if (userId == null) {
            return true;
        }

        String key = userId + ":" + rid;
        long now = System.currentTimeMillis() / 1000;

        Entry prev = seen.put(key, new Entry(now));
        if (prev != null && (now - prev.ts) < WINDOW_SECONDS) {
            response.setStatus(429);
            response.setContentType("application/json;charset=utf-8");
            try { response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后重试\"}"); } catch (java.io.IOException ignored) {}
            return false;
        }

        // 清理过期键（每 100 次随机触发一次）
        if (ThreadLocalRandom.current().nextInt(100) < 5) {
            purge();
        }
        return true;
    }

    private void purge() {
        long cutoff = System.currentTimeMillis() / 1000 - WINDOW_SECONDS;
        seen.entrySet().removeIf(e -> e.getValue().ts < cutoff);
    }

    private record Entry(long ts) {}
}
