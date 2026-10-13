package com.campus.smart.service.ai.impl;

import com.campus.smart.service.RagRetriever;
import com.campus.smart.service.ai.AiTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具：检索校园公告/业务规则知识库
 *
 * <p>与"预检索"互补：系统在发起对话前会先按用户原问题做一次 RAG 预检索，
 * 而本工具允许模型在<b>多轮追问</b>或<b>需要补充信息</b>时主动再检索一次
 * （例如用户先问快递、再追问"那打印呢"，模型可自行检索打印相关公告）。</p>
 *
 * @author 万盛博
 */
@Slf4j
@Component
public class SearchNoticeTool implements AiTool {

    @Autowired
    private RagRetriever ragRetriever;

    /** 工具调用时召回条数（略少于预检索场景，避免上下文过长） */
    private static final int TOP_K = 3;

    @Override
    public String name() {
        return "search_notice";
    }

    @Override
    public String description() {
        return "检索校园公告与业务规则知识库。当用户询问校园服务规则、办理流程、收费标准、"
                + "活动参与方式、二手交易规范等校园知识类问题时调用。"
                + "如果对话前已提供的【参考资料】不足以回答用户的新问题，也可以调用本工具补充检索。";
    }

    @Override
    public Map<String, Object> parameters() {
        Map<String, Object> props = new LinkedHashMap<>();
        Map<String, Object> keyword = new LinkedHashMap<>();
        keyword.put("type", "string");
        keyword.put("description", "检索关键词或用户的完整问题，例如\"打印怎么收费\"\"如何上架二手商品\"");
        props.put("keyword", keyword);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", Arrays.asList("keyword"));
        return schema;
    }

    @Override
    public String execute(Long userId, Map<String, Object> args) {
        Object kw = args.get("keyword");
        String question = kw == null ? "" : String.valueOf(kw).trim();
        if (question.isEmpty()) {
            return "错误：keyword 参数为空，请提供检索关键词。";
        }

        // 复用 RAG 检索器（阈值与生产链路保持一致）
        List<RagRetriever.Retrieved> list = ragRetriever.retrieve(question, TOP_K, 0.003);
        if (list == null || list.isEmpty()) {
            return "检索结果：知识库中没有与\"" + question + "\"相关的资料。"
                    + "请如实告知用户这个问题你暂时回答不了，建议咨询学校相关部门，不要编造。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("检索结果：找到 ").append(list.size()).append(" 条相关资料：\n");
        int i = 1;
        for (RagRetriever.Retrieved r : list) {
            sb.append("资料").append(i++).append("（来源：")
              .append(r.knowledge.getTitle() == null ? "校园公告" : r.knowledge.getTitle())
              .append("）：").append(r.knowledge.getContent()).append("\n\n");
        }
        sb.append("请仅依据以上资料回答，资料中没有的信息明确告知用户。");
        return sb.toString();
    }
}
