package com.campus.smart.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 工具注册表：汇总所有工具、生成 tools schema、统一分发执行
 *
 * <p>Spring 启动时把所有 {@link AiTool} 实现自动注入进来（构造注入 List），
 * 新增工具只需实现接口并加 @Component，无需改动任何调度代码。</p>
 *
 * @author 万盛博
 */
@Slf4j
@Component
public class AiToolRegistry {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 工具名 → 工具实例（LinkedHashMap 保证顺序稳定，便于展示与排查） */
    private final Map<String, AiTool> tools = new LinkedHashMap<>();

    @Autowired
    public AiToolRegistry(List<AiTool> toolList) {
        for (AiTool tool : toolList) {
            tools.put(tool.name(), tool);
        }
        log.info("[AI-Agent] 工具注册完成，共 {} 个：{}", tools.size(), tools.keySet());
    }

    /**
     * 生成 OpenAI 兼容的 tools 数组，交给大模型
     *
     * @return tools 参数结构
     */
    public List<Map<String, Object>> toolSchemas() {
        List<Map<String, Object>> schemas = new ArrayList<>();
        for (AiTool tool : tools.values()) {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", tool.name());
            function.put("description", tool.description());
            function.put("parameters", tool.parameters());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "function");
            item.put("function", function);
            schemas.add(item);
        }
        return schemas;
    }

    /**
     * 执行工具调用（含参数解析与异常兜底）
     *
     * @param name       工具名（模型给的）
     * @param userId     当前登录用户（后端注入）
     * @param argsJson   模型给的参数（JSON 字符串，可能为空）
     * @return 执行结果文本；任何异常都返回可读的错误说明，让模型能自行调整或致歉
     */
    public String execute(String name, Long userId, String argsJson) {
        AiTool tool = tools.get(name);
        if (tool == null) {
            // 模型可能幻觉出不存在的工具名，明确告知它，让它改用可用工具
            log.warn("[AI-Agent] 模型请求了未注册的工具: {}", name);
            return "错误：不存在名为 " + name + " 的工具，可用工具为 " + tools.keySet();
        }
        try {
            Map<String, Object> args = new LinkedHashMap<>();
            if (argsJson != null && !argsJson.trim().isEmpty()) {
                args = objectMapper.readValue(argsJson, Map.class);
            }
            long start = System.currentTimeMillis();
            String result = tool.execute(userId, args);
            log.info("[AI-Agent] 工具 {} 执行完成，耗时 {}ms", name, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("[AI-Agent] 工具 " + name + " 执行失败", e);
            // 把失败原因回传给模型（而不是抛给前端），模型可以换个方式回答或告知用户
            return "错误：工具执行失败（" + e.getMessage() + "），请告知用户稍后再试，不要编造数据";
        }
    }

    /** 工具数量（供健康检查/调试接口使用） */
    public int size() {
        return tools.size();
    }

    /** 工具名列表 */
    public List<String> names() {
        return new ArrayList<>(tools.keySet());
    }
}
