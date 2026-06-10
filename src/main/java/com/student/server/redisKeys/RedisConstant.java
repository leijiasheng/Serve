package com.student.server.redisKeys;

public class RedisConstant {

    public static final String COURSE_HASH_KEY = "course:all";

    public static final String PRODUCT_STOCK_PREFIX = "product:stock:";

    public static final String USER_SNAPPED_PREFIX = "user:snapped:";

    public static final Integer STOCK_EXPIRE_SEC = 21600;

    public static final String EMAIL_CODE_KEY_PREFIX = "emailCode:";

    public static final Integer EMAIL_CODE_EXPIRE_MINUTES = 5;

    public static final String EMAIL_REGISTER_CODE_PREFIX = "register:code:";

    public static final Integer EMAIL_REGISTER_CODE_EXPIRE_MINUTES = 5;

    // ==================== 滑动窗口限流 ====================

    /** 限流 key 前缀 */
    public static final String RATE_LIMIT_KEY_PREFIX = "rate:limiter:snapped:user:";

    /** 滑动窗口大小（毫秒） */
    public static final long RATE_LIMIT_WINDOW_MS = 1000;

    /** 窗口内最大请求数 */
    public static final long RATE_LIMIT_MAX_REQUESTS = 3;

    /** Redis key 过期时间（秒），略大于窗口，确保数据正确清理 */
    public static final long RATE_LIMIT_TTL_SEC = 5;

}
