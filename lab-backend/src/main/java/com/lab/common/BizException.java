package com.lab.common;

/**
 * 业务异常
 */
public class BizException extends RuntimeException {
    private final int code;

    public BizException(String message)              { super(message); this.code = 500; }
    public BizException(int code, String message)    { super(message); this.code = code; }

    public int getCode() { return code; }

    /** 使用 ErrorCode 枚举创建异常 */
    public static BizException of(ErrorCode errorCode) {
        return new BizException(errorCode.getCode(), errorCode.getDefaultMessage());
    }

    /** 使用 ErrorCode 枚举，自定义 message */
    public static BizException of(ErrorCode errorCode, String message) {
        return new BizException(errorCode.getCode(), message);
    }
}
