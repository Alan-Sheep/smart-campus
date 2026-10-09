package com.campus.smart.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求参数解析工具
 *
 * <p>背景：前端（uni-app / 小程序）统一以 {@code Content-Type: application/json}
 * 发送 POST 请求体，而部分接口原先是 {@code @RequestParam}，只接受 query/form 参数，
 * 导致 Spring 抛出 {@code MissingServletRequestParameterException}（HTTP 400）。</p>
 *
 * <p>本工具让同一个接口<b>同时兼容</b>两种传参方式，优先级为：
 * <ol>
 *   <li>URL query 参数 / form 表单参数（兼容 Swagger、curl、小程序旧调用）</li>
 *   <li>JSON 请求体字段（前端当前调用方式）</li>
 * </ol>
 * 这样既修复了前端报错，又不会破坏任何既有调用方。</p>
 *
 * <p>JSON 请求体解析结果会缓存到 request attribute，避免同一请求内重复读取输入流。</p>
 */
public final class ReqParams {

    private static final String BODY_ATTR = "SMART_CAMPUS_PARSED_BODY";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ReqParams() {
    }

    /**
     * 读取并缓存 JSON 请求体（非 JSON 或为空时返回空 Map，不抛异常）
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> body(HttpServletRequest request) {
        Object cached = request.getAttribute(BODY_ATTR);
        if (cached instanceof Map) {
            return (Map<String, Object>) cached;
        }

        Map<String, Object> map = new HashMap<>(8);
        try (BufferedReader reader = request.getReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            if (sb.length() > 0) {
                map = MAPPER.readValue(sb.toString(), new TypeReference<Map<String, Object>>() {
                });
            }
        } catch (Exception ignored) {
            // 请求体不是 JSON（例如表单、空体）：保持空 Map，交由 query 参数兜底
        }

        request.setAttribute(BODY_ATTR, map);
        return map;
    }

    /**
     * 按优先级读取字符串参数：query/form 优先，其次 JSON 请求体
     *
     * @return 参数值；不存在时返回 {@code null}
     */
    public static String getString(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        Object fromBody = body(request).get(name);
        return fromBody == null ? null : String.valueOf(fromBody);
    }

    /**
     * 读取 Long 参数，兼容 {@code "1"} 与数字 {@code 1}
     *
     * @return 解析失败或不存在时返回 {@code null}
     */
    public static Long getLong(HttpServletRequest request, String name) {
        String value = getString(request, name);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 读取 Integer 参数
     *
     * @return 解析失败或不存在时返回 {@code null}
     */
    public static Integer getInteger(HttpServletRequest request, String name) {
        String value = getString(request, name);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 读取 Long 参数并校验必填
     *
     * @throws IllegalArgumentException 参数缺失或非法
     */
    public static Long requireLong(HttpServletRequest request, String name) {
        Long value = getLong(request, name);
        if (value == null) {
            throw new IllegalArgumentException("缺少必要参数：" + name);
        }
        return value;
    }
}
