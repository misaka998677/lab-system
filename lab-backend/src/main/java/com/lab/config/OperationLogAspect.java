package com.lab.config;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.module.system.entity.SysLog;
import com.lab.module.system.service.SysLogService;
import com.lab.security.LoginUser;
import com.lab.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 拦截所有写操作 Controller，异步落库到 sys_log。 */
@Aspect
@Slf4j
@Component
public class OperationLogAspect {

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password", "Password", "PASSWORD", "confirmPassword", "oldPassword", "newPassword");
    private static final Pattern FIELD_RE = Pattern.compile(
        "\"(" + String.join("|", SENSITIVE_FIELDS) + ")\"\\s*:\\s*\"[^\"]*\"");

    private final SysLogService logService;
    private final ObjectMapper  mapper = new ObjectMapper();

    public OperationLogAspect(SysLogService s) { this.logService = s; }

    @Around("@within(org.springframework.web.bind.annotation.RestController) "
          + "&& (@annotation(org.springframework.web.bind.annotation.PostMapping) "
          + " || @annotation(org.springframework.web.bind.annotation.PutMapping) "
          + " || @annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long t = System.currentTimeMillis();
        SysLog l = new SysLog();
        try {
            Object ret = pjp.proceed();
            l.setStatus(1);
            return ret;
        } catch (Throwable e) {
            l.setStatus(0);
            l.setErrorMsg(StrUtil.maxLength(e.getMessage(), 480));
            throw e;
        } finally {
            try { fillBase(l, pjp); l.setCostMs(System.currentTimeMillis() - t); logService.asyncSave(l); }
            catch (Exception ignore) {}
        }
    }

    private void fillBase(SysLog l, ProceedingJoinPoint pjp) {
        LoginUser u = SecurityUtil.current();
        if (u != null) { l.setUserId(u.getUser().getId()); l.setUsername(u.getUsername()); }

        Method m = ((MethodSignature) pjp.getSignature()).getMethod();
        l.setMethod(pjp.getTarget().getClass().getSimpleName() + "#" + m.getName());
        l.setModule(pjp.getTarget().getClass().getPackageName());
        l.setAction(actionOf(m));

        // 对参数做 JSON 序列化后再脱敏（对登录/注册等敏感字段用 *** 替换），
        // 避免密码等敏感信息落库。
        try {
            String raw = StrUtil.maxLength(mapper.writeValueAsString(pjp.getArgs()), 2000);
            String safe = FIELD_RE.matcher(raw).replaceAll(mr ->
                "\"" + mr.group(1) + "\":\"***\"");
            l.setParams(safe);
        } catch (Exception ignore) {}

        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attr != null) {
            HttpServletRequest req = attr.getRequest();
            l.setIp(StrUtil.firstNonBlank(req.getHeader("X-Forwarded-For"), req.getRemoteAddr()));
        }
    }

    private String actionOf(Method m) {
        if (m.isAnnotationPresent(PostMapping.class))   return "CREATE";
        if (m.isAnnotationPresent(PutMapping.class))    return "UPDATE";
        if (m.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        return "OTHER";
    }
}
