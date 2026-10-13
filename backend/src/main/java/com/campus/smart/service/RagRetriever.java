package com.campus.smart.service;

import com.campus.smart.entity.AiKnowledge;
import com.campus.smart.mapper.AiKnowledgeMapper;
import com.campus.smart.utils.LocalVectorizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 检索器：知识片段的向量化召回
 *
 * <p>流程：加载启用中的知识片段 → 向量化缓存 → 用户问题转向量 →
 * 余弦相似度计算 → Top-K 召回（低于阈值过滤，防幻觉）</p>
 *
 * @author 万盛博
 */
@Slf4j
@Service
public class RagRetriever {

    @Autowired
    private AiKnowledgeMapper knowledgeMapper;

    @Autowired
    private LocalVectorizer vectorizer;

    /** 内存向量缓存：片段 id → 向量（应用启动后首次检索时懒加载） */
    private final List<KnowledgeVector> vectorCache = new ArrayList<>();
    private volatile boolean cacheLoaded = false;

    /**
     * 检索最相关的知识片段
     *
     * @param question 用户问题
     * @param topK     召回条数
     * @param minScore 最低相似度阈值（低于此值不返回，防幻觉）
     * @return 按相似度降序的知识片段（可能为空：无相关资料时明确告知模型）
     */
    public List<Retrieved> retrieve(String question, int topK, double minScore) {
        ensureCache();
        if (vectorCache.isEmpty()) {
            log.warn("[RAG] 知识库为空，跳过检索");
            return new ArrayList<>();
        }
        double[] qv = vectorizer.textToVector(question);
        // 全量算相似度（万级数据内存计算毫秒级完成），按分数排序取 Top-K
        List<Retrieved> scored = new ArrayList<>();
        for (KnowledgeVector kv : vectorCache) {
            double score = vectorizer.cosineSimilarity(qv, kv.vector);
            if (score >= minScore) {
                scored.add(new Retrieved(kv.knowledge, score));
            }
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        // 调试日志：Top5 分数
        StringBuilder dbg = new StringBuilder("Top5: ");
        for (int i = 0; i < Math.min(5, scored.size()); i++) {
            dbg.append(String.format("[%d]=%.4f ", scored.get(i).knowledge.getId(), scored.get(i).score));
        }
        log.info("[RAG] {}", dbg);
        List<Retrieved> top = scored.subList(0, Math.min(topK, scored.size()));
        log.info("[RAG] 问题='{}' 召回 {} 条（总分池 {}，阈值 {}）", question, top.size(), scored.size(), minScore);
        return top;
    }

    /** 知识片段有增删改时调用，刷新缓存 */
    public synchronized void refreshCache() {
        cacheLoaded = false;
        ensureCache();
    }

    /** 懒加载知识库到内存 */
    private synchronized void ensureCache() {
        if (cacheLoaded) {
            return;
        }
        vectorCache.clear();
        List<AiKnowledge> list = knowledgeMapper.selectList(
                new LambdaQueryWrapper<AiKnowledge>().eq(AiKnowledge::getEnabled, 1));
        for (AiKnowledge k : list) {
            vectorCache.add(new KnowledgeVector(k, vectorizer.textToVector(k.getContent())));
        }
        cacheLoaded = true;
        log.info("[RAG] 知识库缓存加载完成：{} 条片段", vectorCache.size());
    }

    /** 片段+向量的内存结构 */
    private static class KnowledgeVector {
        final AiKnowledge knowledge;
        final double[] vector;

        KnowledgeVector(AiKnowledge knowledge, double[] vector) {
            this.knowledge = knowledge;
            this.vector = vector;
        }
    }

    /** 检索结果：片段 + 相似度得分 */
    public static class Retrieved {
        public final AiKnowledge knowledge;
        public final double score;

        public Retrieved(AiKnowledge knowledge, double score) {
            this.knowledge = knowledge;
            this.score = score;
        }
    }
}
