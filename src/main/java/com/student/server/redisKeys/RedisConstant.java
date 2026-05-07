package com.student.server.redisKeys;

public class RedisConstant {

    public static final String COURSE_HASH_KEY = "course:all";

    public static final String USER_COURSE_KEY = "user:course";

    public static final String PRODUCT_STOCK_PREFIX = "product:stock:";

    public static final String USER_SNAPPED_PREFIX = "user:snapped:";

    public static final long SNAPPED_EXPIRE_SEC = 86400L;

    public static final Integer STOCK_EXPIRE_SEC = 6;

    public static final String EMAIL_CODE_KEY_PREFIX = "emailCode:";

    public static final Integer EMAIL_CODE_EXPIRE_MINUTES = 5;

}
