package com.campus.smart.service.ai;

import java.math.BigDecimal;

/**
 * AI 工具公共支撑方法
 *
 * <p>多个订单类工具都需要做「状态码 → 中文」、「字符串筛选条件 → 数据库状态码」、
 * 「null → 友好占位」这类转换，抽到此处避免每个工具重复实现。</p>
 *
 * @author 万盛博
 */
public final class AiToolSupport {

    private AiToolSupport() {
    }

    /**
     * 数据库状态码 → 中文（订单类表统一约定：0已取消 1待接单 2进行中 3已完成）
     */
    public static String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0: return "已取消";
            case 1: return "待接单";
            case 2: return "进行中";
            case 3: return "已完成";
            default: return "状态" + status;
        }
    }

    /**
     * 模型给的筛选词 → 数据库状态码；无法识别时返回 null（表示不按状态过滤）
     */
    public static Integer mapStatus(String status) {
        if (status == null) {
            return null;
        }
        switch (status) {
            case "pending": return 1;
            case "processing": return 2;
            case "done": return 3;
            case "canceled": return 0;
            default: return null;
        }
    }

    /** null 安全的文本展示（避免工具输出里出现 "null" 干扰模型理解） */
    public static String textOrNone(String s) {
        return (s == null || s.trim().isEmpty()) ? "无" : s;
    }

    /** null 安全的金额展示 */
    public static String amountOrZero(BigDecimal amount) {
        return amount == null ? "0" : amount.toPlainString();
    }

    /** 时间展示：去掉 ISO 格式里的 T，便于模型朗读与用户阅读 */
    public static String timeText(java.time.LocalDateTime time) {
        return time == null ? "未知" : time.toString().replace("T", " ");
    }
}
