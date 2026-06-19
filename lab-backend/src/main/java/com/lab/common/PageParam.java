package com.lab.common;

/**
 * 分页参数约束工具。
 *
 * <p>统一三个行为：
 * <ul>
 *   <li>pageNum 最小为 1（避免 0 或负数导致 SQL 分页异常）</li>
 *   <li>pageSize 最小为 1，最大为 {@link Constants#PAGE_SIZE_MAX}（默认 100，避免一次性拖回全表拖垮 DB）</li>
 *   <li>若调用方传了超过最大范围的数值，使用 clamp 静默调整，而不是抛异常，以兼容旧前端请求</li>
 * </ul>
 */
public final class PageParam {

    private PageParam() {}

    public static int clampPageNum(int pageNum) {
        return Math.max(Constants.PAGE_NUM_MIN, pageNum);
    }

    public static int clampPageSize(int pageSize) {
        if (pageSize < Constants.PAGE_SIZE_MIN) return Constants.PAGE_SIZE_DEFAULT;
        if (pageSize > Constants.PAGE_SIZE_MAX) return Constants.PAGE_SIZE_MAX;
        return pageSize;
    }

    /** 返回 [pageNum, pageSize]，保证都在合法区间内。 */
    public static int[] clamp(int pageNum, int pageSize) {
        return new int[]{ clampPageNum(pageNum), clampPageSize(pageSize) };
    }
}
