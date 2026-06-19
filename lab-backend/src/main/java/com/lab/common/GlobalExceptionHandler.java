package com.lab.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(basePackages = "com.lab")
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<?> biz(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> denied(AccessDeniedException e) {
        log.warn("[403] {}", e.getMessage());
        return Result.fail(403, "无访问权限");
    }

    @ExceptionHandler(AuthenticationException.class)
    public Result<?> auth(AuthenticationException e) {
        log.warn("[401] {}", e.getMessage());
        return Result.fail(401, "未登录或登录已过期");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<?> valid(Exception e) {
        String msg;
        if (e instanceof MethodArgumentNotValidException me) {
            msg = me.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .orElse("参数错误");
        } else if (e instanceof BindException be) {
            msg = be.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .orElse("参数错误");
        } else {
            msg = "参数错误";
        }
        return Result.fail(400, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> constraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
            .findFirst()
            .map(v -> v.getPropertyPath() + " " + v.getMessage())
            .orElse("参数错误");
        return Result.fail(400, msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> missingParam(MissingServletRequestParameterException e) {
        return Result.fail(400, "缺少必要参数: " + e.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> badBody(HttpMessageNotReadableException e) {
        log.warn("[bad-body] {}", e.getMessage());
        return Result.fail(400, "请求体格式错误");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<?> badMethod(HttpRequestMethodNotSupportedException e) {
        return Result.fail(405, "请求方法不支持: " + e.getMethod());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<?> badMediaType(HttpMediaTypeNotSupportedException e) {
        return Result.fail(415, "请求内容类型不支持");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<?> notFound(NoHandlerFoundException e) {
        return Result.fail(404, "请求路径不存在");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<?> duplicate(DuplicateKeyException e) {
        log.warn("[duplicate-key] {}", e.getMessage());
        String msg = "数据已存在，请检查编号/名称等唯一字段";
        String m = e.getMessage();
        if (m != null) {
            if (m.contains("asset_no"))       msg = "设备编号已存在";
            else if (m.contains("`code`"))    msg = "耗材编号已存在";
            else if (m.contains("username"))  msg = "用户名已存在";
            else if (m.contains("role_code")) msg = "角色编码已存在";
            else if (m.contains("name"))      msg = "名称已存在";
        }
        return Result.fail(409, msg);
    }

    /** 兜底异常：异常详情仅写入日志，不回显给前端，避免泄露表结构/堆栈。 */
    @ExceptionHandler(Exception.class)
    public Result<?> all(HttpServletRequest req, Exception e) {
        log.error("[uncaught] {} -> {}", req.getRequestURI(), e.getMessage(), e);
        return Result.fail(500, "服务器内部错误，请联系管理员");
    }
}
