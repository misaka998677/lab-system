package com.lab.common;

/** 系统常量集合。统一使用本类中的常量，避免在业务代码中散落魔法数字与字符串。 */
public final class Constants {

    private Constants() {}

    // ========== 角色 ==========
    public static final String ROLE_ADMIN    = "ADMIN";
    public static final String ROLE_LABADMIN = "LABADMIN";
    public static final String ROLE_TEACHER  = "TEACHER";
    public static final String ROLE_STUDENT  = "STUDENT";

    // ========== 通用状态 ==========
    public static final int STATUS_OK     = 0; // 启用 / 正常
    public static final int STATUS_OFF    = 1; // 禁用 / 停用

    // ========== 通用分页 ==========
    public static final int PAGE_NUM_MIN      = 1;
    public static final int PAGE_SIZE_MIN     = 1;
    public static final int PAGE_SIZE_DEFAULT = 10;
    public static final int PAGE_SIZE_MAX     = 100;

    // ========== 密码策略 ==========
    public static final int PASSWORD_MIN_LEN = 6;
    public static final int PASSWORD_MAX_LEN = 64;

    // ========== 账号 ==========
    public static final int MAX_LOGIN_FAILS = 5;

    // ========== WebSocket 消息类型 ==========
    public static final String WS_REFRESH_OVERVIEW = "refresh-overview";
    public static final String WS_REFRESH_USAGE    = "refresh-usage";

    // ========== 缓存名 ==========
    public static final String CACHE_MENU_TREE    = "menu_tree";
    public static final String CACHE_DEPT_TREE    = "dept_tree";
    public static final String CACHE_USER_INFO    = "user_info";
    public static final String CACHE_STAT_OVERVIEW = "stat_overview";
    public static final String CACHE_STAT_USAGE   = "stat_usage";

    // ========== 文件 ==========
    public static final String EXCEL_XLSX = ".xlsx";
    public static final String EXCEL_XLS  = ".xls";
}
