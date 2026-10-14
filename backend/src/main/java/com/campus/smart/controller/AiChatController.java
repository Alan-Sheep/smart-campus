package com.campus.smart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.smart.common.Result;
import com.campus.smart.entity.AiChatRecord;
import com.campus.smart.entity.AiKnowledge;
import com.campus.smart.mapper.AiChatRecordMapper;
import com.campus.smart.service.AiAgentService;
import com.campus.smart.service.DeepSeekService;
import com.campus.smart.service.KnowledgeIngestService;
import com.campus.smart.service.RagRetriever;
import com.campus.smart.service.ai.AiToolRegistry;
import com.campus.smart.mapper.AiKnowledgeMapper;
import com.campus.smart.utils.UserContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 智能问答接口（RAG + Agent 工具调用）
 *
 * <p>调用链：用户提问 → Redis 高频缓存命中检查 →
 * （未命中）RAG 预检索召回知识片段 → 交给 {@link AiAgentService} 编排
 * （模型可自主决定直接回答或调用工具查实时数据，最多 3 轮）→ SSE 推送前端 → 落库</p>
 *
 * @author 万盛博
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    /** SSE 连接超时（毫秒）：Agent 可能多轮调用模型，需留足余量 */
    private static final long SSE_TIMEOUT_MS = 60_000L;

    /** RAG 预检索参数：走配置便于调优（避免代码里硬编码与 yml 定义漂移） */
    @Value("${rag.top-k:5}")
    private int ragTopK;

    @Value("${rag.min-score:0.003}")
    private double ragMinScore;

    @Autowired
    private RagRetriever retriever;

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private AiAgentService aiAgentService;

    @Autowired
    private AiToolRegistry toolRegistry;

    @Autowired
    private KnowledgeIngestService ingestService;

    @Autowired
    private AiChatRecordMapper chatRecordMapper;

    @Autowired
    private AiKnowledgeMapper knowledgeMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /** 缓存 key 前缀与有效期 */
    private static final String CACHE_KEY_PREFIX = "campus:ai:qa:v2:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    /** 用于构造/解析缓存 JSON（局部实例，不注册为 Bean，避免污染 Spring MVC 的消息转换器） */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * AI 问答（SSE 流式）
     * 前端订阅事件：tool=调用的工具 message=增量片段 done=完整回答 error=异常
     */
    @GetMapping("/chat")
    public SseEmitter chat(@RequestParam String question) {
        long start = System.currentTimeMillis();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        Long userId = UserContext.getUserId();

        // 1. Redis 缓存命中检查（问题归一化后取 MD5 作 key）
        String cacheKey = CACHE_KEY_PREFIX + md5(normalize(question));
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                // 缓存格式为 JSON：{"a":答案, "t":工具名}；同时兼容早期的纯文本格式
                String cachedAnswer = cached;
                String cachedTools = "";
                if (cached.startsWith("{")) {
                    try {
                        JsonNode node = objectMapper.readTree(cached);
                        cachedAnswer = node.path("a").asText("");
                        cachedTools = node.path("t").asText("");
                    } catch (Exception e) {
                        log.warn("[AI] 缓存内容解析失败，按纯文本处理");
                    }
                }
                // 工具调用标记也要一起回放——否则「命中缓存的回答」与「首次生成的回答」
                // 在同一个前端上表现不一致（前者没有数据来源标记）
                if (!cachedTools.isEmpty()) {
                    emitter.send(SseEmitter.event().name("tool").data(cachedTools));
                }
                // 注意：SSE 以空行分隔事件，缓存里的真实换行必须转义为字面 \n，
                // 否则前端按空行分帧时只拿到第一段（表现为回答被截断）
                String safeAnswer = cachedAnswer.replace("\n", "\\n");
                emitter.send(SseEmitter.event().name("message").data(safeAnswer));
                emitter.send(SseEmitter.event().name("done").data(safeAnswer));
                emitter.complete();
                saveRecord(userId, question, cachedAnswer, null, true,
                        (int) (System.currentTimeMillis() - start));
            } catch (Exception e) {
                log.warn("[AI] 缓存命中推送失败", e);
            }
            return emitter;
        }

        // 2. RAG 预检索：召回知识片段，保证基础问答能力
        List<RagRetriever.Retrieved> retrieved =
                retriever.retrieve(question, ragTopK, ragMinScore);

        // 3. 交给 Agent 编排：模型可自主决定"直接回答"或"调用工具查实时数据"
        String retrievedIds = retrieved.stream()
                .map(r -> r.knowledge.getId().toString())
                .collect(Collectors.joining(","));

        try {
            emitter.onCompletion(() -> { });
            emitter.onTimeout(() -> log.warn("[AI] SSE 超时"));
            doAgentChat(retrieved, emitter, cacheKey, userId, question, retrievedIds, start);
        } catch (Exception e) {
            log.error("[AI] 问答处理异常", e);
            saveRecord(userId, question, null, retrievedIds, false,
                    (int) (System.currentTimeMillis() - start));
        }
        return emitter;
    }

    /**
     * Agent 问答：多轮工具调用 + 结果推送
     *
     * <p>为什么用"同步跑完 Agent 再分片推送"而不是全程流式：
     * Agent 每一轮都要拿到<b>完整</b>的模型响应，才能判断这次是"直接回答"还是"要调工具"，
     * 中间过程无法流式；只有最终答案需要打字机效果。</p>
     */
    private void doAgentChat(List<RagRetriever.Retrieved> retrieved,
                             SseEmitter emitter, String cacheKey,
                             Long userId, String question,
                             String retrievedIds, long start) {
        new Thread(() -> {
            try {
                // 执行 Agent：内部可能经历多轮"模型决策 → 工具执行 → 结果回填"
                AiAgentService.AgentResult result =
                        aiAgentService.run(question, userId, retrieved);
                String answer = result.answer();

                // 缓存（TTL 24h）：答案 + 工具调用信息一起存，
                // 保证命中缓存时前端能回放出与首次生成一致的"数据来源"标记
                redisTemplate.opsForValue().set(cacheKey,
                        buildCacheValue(answer, result.toolNames()), CACHE_TTL);
                int duration = (int) (System.currentTimeMillis() - start);
                saveRecord(userId, question, answer, retrievedIds, false, duration);

                // 发生过工具调用就先推一个 tool 事件（前端可提示"正在查询订单…"）
                if (result.usedTools()) {
                    emitter.send(SseEmitter.event().name("tool").data(result.toolNames()));
                }
                // 推送：先按片段推 message 模拟流式，再发 done
                for (int i = 0; i < answer.length(); i += 20) {
                                String part = answer.substring(i, Math.min(i + 20, answer.length())).replace("\n", "\\n");
                    emitter.send(SseEmitter.event().name("message").data(part));
                    Thread.sleep(30);
                }
                emitter.send(SseEmitter.event().name("done").data(answer.replace("\n", "\\n")));
                emitter.complete();
            } catch (Exception e) {
                log.error("[AI] 生成/推送异常", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("AI 助手开小差了，请稍后再试"));
                    emitter.complete();
                } catch (Exception ignore) { }
            }
        }, "ai-answer").start();
    }

    /**
     * 查看当前注册的 AI 工具清单（调试/演示用）
     *
     * <p>返回内容就是每次请求实际发给大模型的 tools 数组——
     * 可以直观看到"模型能调哪些工具、每个工具的用途与参数结构"。</p>
     */
    @GetMapping("/agent/tools")
    public Result<List<Map<String, Object>>> agentTools() {
        return Result.success(toolRegistry.toolSchemas());
    }

    /**
     * 构造缓存值：把「答案 + 工具调用信息」序列化为 JSON
     *
     * <p>缓存里必须连"用了哪些工具"一起存——否则命中缓存时前端拿不到工具标记，
     * 同一个问题第二次提问的界面表现会和第一次不一致。</p>
     */
    private String buildCacheValue(String answer, String tools) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("a", answer);
            map.put("t", tools == null ? "" : tools);
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("[AI] 缓存序列化失败，降级为纯文本存储", e);
            return answer;
        }
    }

    /** 落库对话记录 */
    private void saveRecord(Long userId, String question, String answer,
                            String retrievedIds, boolean hitCache, int durationMs) {
        try {
            AiChatRecord record = new AiChatRecord();
            record.setUserId(userId);
            record.setQuestion(question);
            record.setAnswer(answer);
            record.setRetrievedIds(retrievedIds);
            record.setHitCache(hitCache ? 1 : 0);
            record.setDurationMs(durationMs);
            record.setModel("deepseek-chat");
            record.setStatus(answer != null ? 1 : 0);
            chatRecordMapper.insert(record);
        } catch (Exception e) {
            log.warn("[AI] 对话记录落库失败", e);
        }
    }

    /** 知识库重建（管理员/开发用）：从公告全量刷新 */
    @PostMapping("/knowledge/rebuild")
    public Result<Integer> rebuildKnowledge() {
        int count = ingestService.ingestFromNotices();
        return Result.success(count);
    }

    /** 知识库规模查询 */
    @GetMapping("/knowledge/stats")
    public Result<Long> knowledgeStats() {
        return Result.success(knowledgeMapper.selectCount(
                new LambdaQueryWrapper<AiKnowledge>().eq(AiKnowledge::getEnabled, 1)));
    }

    /** 问题归一化：去空白/全半角统一，提升缓存命中率 */
    private String normalize(String q) {
        return q == null ? "" : q.trim().replaceAll("\\s+", "");
    }

    /** MD5 摘要 */
    private String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }
}
