package com.lab.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结构
 */
@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String  message;
    private T       data;

    public static <T> Result<T> ok()                    { return build(200, "success", null); }
    public static <T> Result<T> ok(T data)              { return build(200, "success", data); }
    public static <T> Result<T> ok(String msg, T data)  { return build(200, msg, data); }
    public static <T> Result<T> fail(String msg)        { return build(500, msg, null); }
    public static <T> Result<T> fail(int code, String msg) { return build(code, msg, null); }

    private static <T> Result<T> build(int code, String msg, T data) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = msg;
        r.data = data;
        return r;
    }
}
