package com.student.server.util;


import java.util.UUID;


public class UUIDUtils {

    public static String generateOrderId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
