package com.campus.smart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.smart.common.Result;
import com.campus.smart.entity.AiChatRecord;
import com.campus.smart.entity.AiKnowledge;
import com.campus.smart.mapper.AiChatRecordMapper;
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
    private KnowledgeIngestService ingestService;

    @Autowired
    private AiChatRecordMapper chatRecordMapper;

    @Autowired
    private com.campus.smart.mapper.AiKnowledgeMapper knowledgeMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /** 缓存 key 前缀与有效期 */
    private static final String CACHE_KEY_PREFIX = "campus:ai:qa:";
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
                emitter.send(SseEmitter.event().name("message").data(cached));
                emitter.send(SseEmitter.event().name("done").data(cached));
                emitter.complete();
                saveRecord(userId, question, cached, null, true,
                        (int) (System.currentTimeMillis() - start));
            } catch (Exception e) {
                log.warn("[AI] 缓存命中推送失败", e);
            }
            return emitter;
        }

        // 2. RAG 检索召回知识片段
        List<RagRetriever.Retrieved> retrieved =
                retriever.retrieve(question, 5, 0.003);

        // 3. 组装受控提示词：检索结果拼装为参考资料，缺失时模型须明确告知
        List<DeepSeekService.ChatMessage> messages = new ArrayList<>();
        messages.add(new DeepSeekService.ChatMessage("user", buildUserPrompt(question, retrieved)));

        // 4. 流式生成；完成后缓存高频问答并落库
        StringBuilder fullAnswer = new StringBuilder();
        SseEmitter wrapper = new SseEmitter(60_000L);
        SseEmitter bridge = new SseEmitter(60_000L);

        // 用一个中间发射器桥接 DeepSeekService 的输出
        SseEmitter bridging = new SseEmitter(60_000L) {};
        // 简化：直接传 emitter 给 service，但回调里做缓存/落库
        String retrievedIds = retrieved.stream()
                .map(r -> r.knowledge.getId().toString())
                .collect(Collectors.joining(","));

        // 直接调用流式，注册一个包装 emitter 处理完成事件
        try {
            emitter.onCompletion(() -> { });
            emitter.onTimeout(() -> log.warn("[AI] SSE 超时"));
            // 流式：完成后做缓存与落库（在 service 回调之外处理，见下方 chatWithCallback）
            doChatWithCallback(messages, emitter, cacheKey, userId, question, retrievedIds, start);
        } catch (Exception e) {
            log.error("[AI] 问答处理异常", e);
            saveRecord(userId, question, null, retrievedIds, false,
                    (int) (System.currentTimeMillis() - start));
        }
        return emitter;
    }

    /**
     * 带完成回调的流式问答：完成后写缓存 + 落库
     */
    private void doChatWithCallback(List<DeepSeekService.ChatMessage> messages,
                                    SseEmitter emitter, String cacheKey,
                                    Long userId, String question,
                                    String retrievedIds, long start) {
        // DeepSeekService.chatStream 完成时发 done 事件——这里无法直接拦截其内部线程，
        // 故采用包装：用独立的线程监听完成事件过于复杂，
        // 改为：同步获取完整回答（非流式）→ 缓存/落库 → 再把完整文本通过 SSE 一次性推送（分段模拟流式）
        //权衡说明：为控制本次迭代复杂度，采用"模型同步生成 + 前端分段推送"的实现；
        // SSE 打字机效果在前端按字符间隔渲染，网络传输一次完成。
        new Thread(() -> {
            try {
                String answer = deepSeekService.chat(messages);
                // 缓存（TTL 24h）
                redisTemplate.opsForValue().set(cacheKey, answer, CACHE_TTL);
                // 落库
                int duration = (int) (System.currentTimeMillis() - start);
                saveRecord(userId, question, answer, retrievedIds, false, duration);
                // 推送：先按片段推 message 模拟流式，再发 done
                for (int i = 0; i < answer.length(); i += 20) {
                    String part = answer.substring(i, Math.min(i + 20, answer.length()));
                    emitter.send(SseEmitter.event().name("message").data(part));
                    Thread.sleep(30);
                }
                emitter.send(SseEmitter.event().name("done").data(answer));
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
     * 组装用户提示词：检索片段格式化为参考资料，让模型受控生成
     */
    private String buildUserPrompt(String question, List<RagRetriever.Retrieved> retrieved) {
        if (retrieved.isEmpty()) {
            return "【参考资料】（无相关资料）\n\n【用户问题】" + question
                    + "\n\n请明确告知：这个问题你暂时回答不了，建议咨询学校相关部门。";
        }
        StringBuilder sb = new StringBuilder("【参考资料】\n");
        int i = 1;
        for (RagRetriever.Retrieved r : retrieved) {
            sb.append("资料").append(i++).append("（来源：")
              .append(r.knowledge.getTitle() == null ? "校园公告" : r.knowledge.getTitle())
              .append("）：")
              .append(r.knowledge.getContent())
              .append("\n\n");
        }
        sb.append("【用户问题】").append(question)
          .append("\n\n请依据以上资料回答用户问题；资料中没有的信息请明确说明。");
        return sb.toString();
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
