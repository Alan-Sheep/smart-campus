package com.campus.smart.service.ai;

import java.util.Map;

/**
 * AI 可调用工具（Agent Tool）统一接口
 *
 * <p>设计要点：
 * 1. 工具通过 {@link #name()}、{@link #description()}、{@link #parameters()} 自我描述，
 *    由 {@link AiToolRegistry} 汇总成 OpenAI 兼容的 tools 数组交给大模型，
 *    模型据此决定"调不调、调哪个、传什么参数"；
 * 2. {@link #execute(Long, Map)} 的第一个参数 userId <b>由后端从 JWT 中注入</b>，
 *    <b>不放进 parameters schema</b>——避免让模型自行决定查询谁的数据，从设计上杜绝越权；
 * 3. 当前注册的工具<b>全部是只读查询</b>，删除、支付等高危操作永不进入工具清单。</p>
 *
 * @author 万盛博
 */
public interface AiTool {

    /** 工具名（给模型看的唯一标识，用英文下划线命名） */
    String name();

    /** 工具用途描述（模型据此判断何时调用，描述写得越清楚路由越准） */
    String description();

    /** 参数 JSON Schema（OpenAI function calling 的 parameters 字段结构） */
    Map<String, Object> parameters();

    /**
     * 执行工具
     *
     * @param userId 当前登录用户 ID（后端注入，模型无法指定）
     * @param args   模型填写的参数（已解析为 Map）
     * @return 执行结果（建议返回结构化文本/JSON，会作为 role=tool 的消息回传给模型）
     */
    String execute(Long userId, Map<String, Object> args);
}
