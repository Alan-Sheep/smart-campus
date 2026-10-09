package com.campus.smart.utils;

/**
 * 管理员上下文工具类
 * 用于在当前线程中存储和获取当前登录的管理员ID
 */
public class AdminContext {

    private static final ThreadLocal<Long> CURRENT_ADMIN_ID = new ThreadLocal<>();

    /**
     * 设置当前管理员ID
     */
    public static void setAdminId(Long adminId) {
        CURRENT_ADMIN_ID.set(adminId);
    }

    /**
     * 获取当前管理员ID
     */
    public static Long getAdminId() {
        return CURRENT_ADMIN_ID.get();
    }

    /**
     * 移除当前线程中的管理员ID
     */
    public static void remove() {
        CURRENT_ADMIN_ID.remove();
    }
}
