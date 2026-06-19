package com.lab.common;

/** 字符串 / 常用工具方法。项目内避免散落的魔法数字与空字符串。 */
public final class StringUtils {

    private StringUtils() {}

    public static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    public static boolean isNotBlank(String s) { return !isBlank(s); }
    public static String orEmpty(String s) { return s == null ? "" : s; }
    public static String orDefault(String s, String def) { return isBlank(s) ? def : s; }

    /** 用在 MyBatis XML 的模糊查询前后拼接（避免在 XML 中写 CONCAT('%', ?, '%')）。 */
    public static String like(String raw) {
        if (raw == null) return null;
        return "%" + raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    /** 手机号脱敏：138****1234。 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 邮箱脱敏：a***@example.com。 */
    public static String maskEmail(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "***" + email.substring(at);
    }

    /** 将 pageNum / pageSize 夹到合法区间。 */
    public static int clampPageNum(int pageNum) {
        return Math.max(Constants.PAGE_NUM_MIN, pageNum);
    }
    public static int clampPageSize(int pageSize) {
        if (pageSize < Constants.PAGE_SIZE_MIN) return Constants.PAGE_SIZE_DEFAULT;
        if (pageSize > Constants.PAGE_SIZE_MAX) return Constants.PAGE_SIZE_MAX;
        return pageSize;
    }
}
