package com.campus.smart.utils;

/**
 * 学校上下文，用于存储当前请求的学校ID
 */
public class SchoolContext {

    private static final ThreadLocal<Long> CURRENT_SCHOOL_ID = new ThreadLocal<>();

    public static void setSchoolId(Long schoolId) {
        CURRENT_SCHOOL_ID.set(schoolId);
    }

    public static Long getSchoolId() {
        return CURRENT_SCHOOL_ID.get();
    }

    public static void remove() {
        CURRENT_SCHOOL_ID.remove();
    }
}
