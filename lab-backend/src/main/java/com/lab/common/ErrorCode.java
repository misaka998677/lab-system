package com.lab.common;

/**
 * 统一错误码枚举
 *
 * <ul>
 *   <li>1xxx - 参数/校验错误</li>
 *   <li>2xxx - 认证/授权错误</li>
 *   <li>4xxx - 业务逻辑错误</li>
 *   <li>5xxx - 系统/服务器错误</li>
 * </ul>
 */
public enum ErrorCode {

    // ==================== 参数/校验错误 (1xxx) ====================
    VALIDATION_ERROR(1000, "参数校验失败"),
    INVALID_PARAMETER(1001, "参数无效"),
    MISSING_PARAMETER(1002, "缺少必要参数"),

    // ==================== 认证/授权错误 (2xxx) ====================
    UNAUTHORIZED(2001, "未登录或登录已过期"),
    FORBIDDEN(2003, "无访问权限"),
    TOKEN_INVALID(2004, "Token无效或已过期"),
    ACCOUNT_DISABLED(2005, "账号已禁用"),

    // ==================== 业务逻辑错误 (4xxx) ====================
    NOT_FOUND(4001, "资源不存在"),
    ALREADY_EXISTS(4002, "资源已存在"),
    STATUS_ILLEGAL(4003, "状态不合法"),

    // 通用业务错误（message可自定义）
    BIZ_ERROR(4000, "业务处理失败"),

    // ==================== 系统/服务器错误 (5xxx) ====================
    INTERNAL_ERROR(5000, "服务器内部错误"),
    SERVICE_UNAVAILABLE(5001, "服务暂不可用"),
    DB_ERROR(5002, "数据库操作失败"),

    // ==================== 自定义业务错误码 (在业务代码中使用 BizException(code, msg)) ====================

    // 通用业务错误码范围：400-499
    USERNAME_OR_PASSWORD_ERROR(401, "账号或密码错误"),
    USERNAME_EXISTS(402, "用户名已存在"),
    PASSWORD_TOO_SHORT(403, "密码长度不足"),
    PASSWORD_MISMATCH(404, "两次输入的密码不一致"),
    USERNAME_REQUIRED(405, "用户名不能为空"),

    // 实验室相关 410-419
    LAB_NOT_FOUND(410, "实验室不存在"),
    LAB_NOT_MANAGED(411, "无权限管理该实验室"),
    LAB_CAPACITY_EXCEEDED(412, "预约人数超过实验室容量"),

    // 设备相关 420-429
    DEVICE_NOT_FOUND(420, "设备不存在"),
    DEVICE_NOT_MANAGED(421, "无权限管理该设备"),
    DEVICE_IN_USE(422, "设备正在使用中"),

    // 预约相关 430-439
    RESERVATION_NOT_FOUND(430, "预约记录不存在"),
    RESERVATION_NOT_YOURS(431, "无权操作他人的预约"),
    RESERVATION_TIME_CONFLICT(432, "预约时间冲突"),
    RESERVATION_TIME_ILLEGAL(433, "预约时间不合法"),
    RESERVATION_NOT_PENDING(434, "预约状态不是待审核"),
    RESERVATION_NOT_APPROVED(435, "预约未通过审核"),
    RESERVATION_ALREADY_CHECKED_IN(436, "已签到，无法取消"),

    // 耗材相关 440-449
    ITEM_NOT_FOUND(440, "耗材不存在"),
    STOCK_INSUFFICIENT(441, "库存不足"),
    ITEM_IN_USE(442, "耗材正在使用中"),

    // 账户锁定相关 450-459
    ACCOUNT_LOCKED(450, "账号已锁定"),
    TOO_MANY_FAILS(451, "尝试次数过多，请稍后再试"),

    // 角色配置 460-469
    ROLE_NOT_FOUND(460, "角色不存在"),
    ROLE_CONFIG_MISSING(461, "角色配置缺失"),

    // 用户相关 470-479
    USER_NOT_FOUND(470, "用户不存在"),
    USER_DISABLED(471, "用户已禁用"),
    STUDENT_ONLY(472, "仅支持学生注册"),
    TEACHER_NEED_APPROVAL(473, "教师账号需管理员审核"),

    // 部门相关 480-489
    DEPT_NOT_FOUND(480, "部门不存在"),
    DEPT_HAS_USERS(481, "部门下存在用户，无法删除");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() { return code; }
    public String getDefaultMessage() { return defaultMessage; }

    /**
     * 创建 BizException，message 使用枚举默认值
     */
    public BizException ex() {
        return new BizException(code, defaultMessage);
    }

    /**
     * 创建 BizException，自定义 message
     */
    public BizException ex(String message) {
        return new BizException(code, message);
    }
}
