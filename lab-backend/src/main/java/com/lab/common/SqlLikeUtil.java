package com.lab.common;

/**
 * MyBatis LIKE 查询辅助：将 '%'、'_'、'\' 等通配符进行转义，
 * 避免用户输入的关键字被当作 SQL 通配符处理（防止全表扫描风险）。
 * 使用示例：
 *   String kw = SqlLikeUtil.escape(rawKeyword);
 *   mapper.page("%" + kw + "%", ...);
 */
public final class SqlLikeUtil {

    private SqlLikeUtil() {}

    public static String escape(String keyword) {
        if (keyword == null || keyword.isEmpty()) return keyword;
        // 先转义反斜杠，再转义 % 和 _；SQL 侧需 ESCAPE '\' 或不指定
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /** 返回两端加通配符的已转义字符串；空输入返回 null。 */
    public static String contains(String keyword) {
        String e = escape(keyword);
        return (e == null || e.isEmpty()) ? null : "%" + e + "%";
    }
}
