package com.example.bookrunner.config;

/**
 * Tên các cache dùng cho tầng gợi ý.
 * Đặt tập trung để tránh gõ sai chuỗi giữa {@code @Cacheable} và cấu hình TTL.
 */
public final class CacheNames {

    /** Gợi ý cá nhân hoá theo user. */
    public static final String FOR_YOU = "recsys:for_you";

    /** Sách tương tự (Content-Based). */
    public static final String RELATED = "recsys:related";

    /** Sách thường được mua cùng (Co-Purchase). */
    public static final String FBT = "recsys:fbt";

    /** Sách bán chạy / cold-start. */
    public static final String TRENDING = "recsys:trending";

    private CacheNames() {
    }
}
