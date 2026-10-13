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
import com.campus.smart.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 智能问答接口（RAG）
 *
 * <p>调用链：用户提问 → Redis 高频缓存命中检查 →
 * （未命中）RAG 检索召回知识片段 → 组装受控提示词 →
 * DeepSeek 流式生成 → SSE 推送前端 → 异步落库</p>
 *
 * @author 万盛博
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    @Autowired
    private RagRetriever retriever;

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private AiAgentService aiAgentService;

    @Autowired
    private com.campus.smart.service.ai.AiToolRegistry toolRegistry;

    @Autowired
    private KnowledgeIngestService ingestService;

    @Autowired
    private AiChatRecordMapper chatRecordMapper;

    @Autowired
    private com.campus.smart.mapper.AiKnowledgeMapper knowledgeMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /** 缓存 key 前缀与有效期 */
    private static final String CACHE_KEY_PREFIX = "campus:ai:qa:v2:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    /**
     * AI 问答（SSE 流式）
     * 前端用 EventSource 订阅：message=增量片段 done=完整回答 error=异常
     */
    @GetMapping("/chat")
    public SseEmitter chat(@RequestParam String question) {
        long start = System.currentTimeMillis();
        SseEmitter emitter = new SseEmitter(60_000L);
        Long userId = UserContext.getUserId();

        // 1. Redis 缓存命中检查（问题归一化后取 MD5 作 key）
        String cacheKey = CACHE_KEY_PREFIX + md5(normalize(question));
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                // 注意：SSE 以空行分隔事件，缓存里的真实换行必须转义为字面 \n，
                // 否则前端按空行分帧时只拿到第一段（表现为回答被截断）
                String safeCached = cached.replace("\n", "\\n");
                emitter.send(SseEmitter.event().name("message").data(safeCached));
                emitter.send(SseEmitter.event().name("done").data(safeCached));
                emitter.complete();
                saveRecord(userId, question, cached, null, true,
                        (int) (System.currentTimeMillis() - start));
            } catch (Exception e) {
                log.warn("[AI] 缓存命中推送失败", e);
            }
            return emitter;
        }

        // 2. RAG 预检索：召回知识片段，保证基础问答能力
        List<RagRetriever.Retrieved> retrieved =
                retriever.retrieve(question, 5, 0.003);

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

                // 缓存（TTL 24h）：只缓存最终答案，命中时直接返回、不再走 Agent
                redisTemplate.opsForValue().set(cacheKey, answer, CACHE_TTL);
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
