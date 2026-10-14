package com.campus.smart.service.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * AI 工具公共支撑方法单元测试
 *
 * <p>这些转换方法直接决定工具返回给大模型的内容质量——
 * 状态码错映射会导致模型向用户播报错误的订单状态。</p>
 *
 * @author 万盛博
 */
@DisplayName("AiToolSupport · 状态映射与展示格式化")
class AiToolSupportTest {

    // ===== 状态码 → 中文（订单表统一约定：0已取消 1待接单 2进行中 3已完成）=====

    @Test
    @DisplayName("状态码正确映射为中文")
    void statusTextShouldMapCorrectly() {
        assertEquals("已取消", AiToolSupport.statusText(0));
        assertEquals("待接单", AiToolSupport.statusText(1));
        assertEquals("进行中", AiToolSupport.statusText(2));
        assertEquals("已完成", AiToolSupport.statusText(3));
    }

    @Test
    @DisplayName("未知状态码不抛异常，降级为可读文本")
    void unknownStatusShouldDegradeGracefully() {
        assertEquals("状态99", AiToolSupport.statusText(99));
        assertEquals("未知", AiToolSupport.statusText(null));
    }

    // ===== 模型给的筛选词 → 数据库状态码 =====

    @Test
    @DisplayName("筛选词正确映射为状态码")
    void mapStatusShouldMapCorrectly() {
        assertEquals(Integer.valueOf(1), AiToolSupport.mapStatus("pending"));
        assertEquals(Integer.valueOf(2), AiToolSupport.mapStatus("processing"));
        assertEquals(Integer.valueOf(3), AiToolSupport.mapStatus("done"));
        assertEquals(Integer.valueOf(0), AiToolSupport.mapStatus("canceled"));
    }

    @Test
    @DisplayName("无法识别的筛选词返回 null（表示不按状态过滤，而不是报错）")
    void unknownFilterShouldReturnNull() {
        assertNull(AiToolSupport.mapStatus("unknown"));
        assertNull(AiToolSupport.mapStatus(null));
    }

    // ===== 展示格式化：避免把 "null" 直接喂给模型 =====

    @Test
    @DisplayName("空值统一展示为「无」，防止模型读到 null 字符串")
    void textOrNoneShouldHandleNull() {
        assertEquals("顺丰", AiToolSupport.textOrNone("顺丰"));
        assertEquals("无", AiToolSupport.textOrNone(null));
        assertEquals("无", AiToolSupport.textOrNone("   "));
    }

    @Test
    @DisplayName("金额空值展示为 0")
    void amountOrZeroShouldHandleNull() {
        assertEquals("8.10", AiToolSupport.amountOrZero(new BigDecimal("8.10")));
        assertEquals("0", AiToolSupport.amountOrZero(null));
    }

    @Test
    @DisplayName("时间去掉 ISO 的 T，便于模型朗读")
    void timeTextShouldBeReadable() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 13, 14, 30, 0);
        assertEquals("2026-10-13 14:30", AiToolSupport.timeText(time));
        assertEquals("未知", AiToolSupport.timeText(null));
    }
}
