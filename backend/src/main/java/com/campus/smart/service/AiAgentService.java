package com.campus.smart.service;

import com.campus.smart.service.ai.AiToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI Agent 编排器：负责"模型决策 → 执行工具 → 回传结果 → 再决策"的多轮循环
 *
 * <p><b>完整调用链</b>：
 * <pre>
 * 用户提问
 *   ↓
 * RAG 预检索（把相关资料塞进 user 消息，保证基础问答能力）
 *   ↓
 * ┌─→ 调用大模型（携带 tools 数组）
 * │     ↓
 * │   模型返回什么？
 * │     ├─ 纯文本 → 这就是最终回答，结束
 * │     └─ tool_calls → 后端执行工具（userId 从 JWT 注入）
 * │            ↓
 * │        工具结果作为 role=tool 消息追加进上下文
 * │            ↓
 * └────────┘（回到开头，最多循环 MAX_TOOL_ROUNDS 次）
 * </pre>
 *
 * <p><b>为什么大模型能"自己判断"要不要调工具</b>：请求里带了 tools 数组，
 * 每个工具都声明了名称、用途描述和参数结构；模型根据用户问题的语义，
 * 自行决定是直接回答还是先要数据。若判断需要数据，它返回的不是文字，
 * 而是 tool_calls 结构（工具名 + 参数 JSON）。</p>
 *
 * <p><b>安全设计</b>：
 * ① 工具参数里<b>不含 userId</b>，由后端从登录态注入，杜绝"让模型查别人数据"；
 * ② 注册的工具全部是只读查询，写操作永不进入工具清单；
 * ③ 循环有上限，防止模型反复调用导致成本失控。</p>
 *
 * @author 万盛博
 */
@Slf4j
@Service
public class AiAgentService {

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private AiToolRegistry toolRegistry;

    /** 工具调用最大轮次：3 轮足够覆盖"查订单 + 查公告"这类组合，同时防止死循环 */
    private static final int MAX_TOOL_ROUNDS = 3;

    /**
     * 执行一次 Agent 问答
     *
     * @param question     用户问题
     * @param userId       当前登录用户（注入给工具，模型不可指定）
     * @param preRetrieved RAG 预检索到的资料片段（已拼进提示词）
     * @return 最终回答 + 工具调用轨迹
     */
    public AgentResult run(String question, Long userId, List<RagRetriever.Retrieved> preRetrieved) {
        List<DeepSeekService.ChatMessage> messages = new ArrayList<>();
        messages.add(new DeepSeekService.ChatMessage("system", SYSTEM_PROMPT));
        messages.add(new DeepSeekService.ChatMessage("user", buildUserPrompt(question, preRetrieved)));

        List<Map<String, Object>> tools = toolRegistry.toolSchemas();
        List<TraceStep> trace = new ArrayList<>();

        for (int round = 1; round <= MAX_TOOL_ROUNDS; round++) {
            DeepSeekService.ChatResult result;
            try {
                result = deepSeekService.chatWithTools(messages, tools);
            } catch (Exception e) {
                log.error("[AI-Agent] 第 {} 轮模型调用失败", round, e);
                // 降级：把已获得的信息用兜底话术回复，不让用户看到空白
                return new AgentResult(fallbackAnswer(trace), trace);
            }

            // 情况一：模型直接回答（没有工具调用请求）→ 这就是最终答案
            if (!result.hasToolCalls()) {
                log.info("[AI-Agent] 第 {} 轮模型直接回答，工具调用 {} 次", round, trace.size());
                return new AgentResult(result.content(), trace);
            }

            // 情况二：模型要求调用工具
            // 重要：必须把 assistant 的 tool_calls 消息放进上下文，
            // 否则下一轮请求里 role=tool 的消息会缺少对应的 tool_call_id，接口会报错
            messages.add(DeepSeekService.ChatMessage.assistantToolCalls(result.toolCalls()));

            for (DeepSeekService.ToolCall call : result.toolCalls()) {
                long t0 = System.currentTimeMillis();
                String output = toolRegistry.execute(call.name(), userId, call.arguments());
                trace.add(new TraceStep(call.name(), call.arguments(), output,
                        (int) (System.currentTimeMillis() - t0)));
                // 工具结果回填：role=tool + tool_call_id，模型据此组织自然语言回答
                messages.add(DeepSeekService.ChatMessage.toolResult(call.id(), output));
            }
        }

        // 达到轮次上限仍未收敛：给一个明确的兜底，而不是无限循环
        log.warn("[AI-Agent] 达到工具调用轮次上限 {}，主动终止", MAX_TOOL_ROUNDS);
        return new AgentResult(
                "这个问题需要查询的信息比较多，我先帮你查到这些，你可以再问得更具体一点～\n\n"
                        + summarizeTrace(trace), trace);
    }

    /** 系统提示词：角色 + 工具使用规则 + 防幻觉约束 */
    private static final String SYSTEM_PROMPT =
            "你是智慧校园平台的学生助手，名字叫\"小智\"。\n"
            + "【可用能力】你可以调用工具查询实时数据。规则：\n"
            + "1. 当用户询问自己的快递代取订单、跑腿订单状态（如\"我的快递到哪了\"\"我的跑腿订单\"），"
            + "必须调用对应的查询工具获取真实数据后再回答，禁止凭猜测编造订单信息；\n"
            + "2. 当用户询问校园规则、办理流程、收费标准等知识类问题时，"
            + "优先使用本条消息中提供的【参考资料】；若资料不足以回答用户的新问题，可调用 search_notice 补充检索；\n"
            + "3. 如果工具返回\"没有找到订单\"，就如实告知用户没有查到，不要编造；\n"
            + "4. 涉及具体数据（价格、时间、地点、订单状态、取件码）必须与工具返回结果或参考资料完全一致；\n"
            + "5. 回答简洁友好、使用中文，不要暴露工具名称和调用细节。";

    /** 组装用户消息：问题 + RAG 预检索资料 */
    private String buildUserPrompt(String question, List<RagRetriever.Retrieved> retrieved) {
        StringBuilder sb = new StringBuilder();
        if (retrieved == null || retrieved.isEmpty()) {
            sb.append("【参考资料】（无相关资料）\n\n");
        } else {
            sb.append("【参考资料】\n");
            int i = 1;
            for (RagRetriever.Retrieved r : retrieved) {
                sb.append("资料").append(i++).append("（来源：")
                  .append(r.knowledge.getTitle() == null ? "校园公告" : r.knowledge.getTitle())
                  .append("）：").append(r.knowledge.getContent()).append("\n\n");
            }
        }
        sb.append("【用户问题】").append(question);
        return sb.toString();
    }

    /** 模型调用失败时的兜底：把已查到的工具结果整理给用户 */
    private String fallbackAnswer(List<TraceStep> trace) {
        if (trace.isEmpty()) {
            return "AI 助手开小差了，请稍后再试～";
        }
        return "我帮你查到了以下信息：\n\n" + summarizeTrace(trace);
    }

    /** 把工具执行结果整理成可读文本（用于兜底回复） */
    private String summarizeTrace(List<TraceStep> trace) {
        StringBuilder sb = new StringBuilder();
        for (TraceStep step : trace) {
            sb.append(step.output).append("\n");
        }
        return sb.toString();
    }

    /** Agent 执行结果 */
    public static class AgentResult {
        private final String answer;
        private final List<TraceStep> trace;

        public AgentResult(String answer, List<TraceStep> trace) {
            this.answer = answer;
            this.trace = trace;
        }

        public String answer() { return answer; }

        public List<TraceStep> trace() { return trace; }

        /** 本次是否发生了工具调用 */
        public boolean usedTools() {
            return trace != null && !trace.isEmpty();
        }

        /** 调用过的工具名（去重，用于前端提示与落库） */
        public String toolNames() {
            if (trace == null || trace.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (TraceStep s : trace) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(s.toolName);
            }
            return sb.toString();
        }
    }

    /** 单次工具调用轨迹（可用于落库与前端展示） */
    public static class TraceStep {
        private final String toolName;
        private final String arguments;
        private final String output;
        private final int durationMs;

        public TraceStep(String toolName, String arguments, String output, int durationMs) {
            this.toolName = toolName;
            this.arguments = arguments;
            this.output = output;
            this.durationMs = durationMs;
        }

        public String toolName() { return toolName; }

        public String arguments() { return arguments; }

        public String output() { return output; }

        public int durationMs() { return durationMs; }
    }
}
