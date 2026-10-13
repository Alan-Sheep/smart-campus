package com.campus.smart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.smart.entity.AiKnowledge;
import com.campus.smart.entity.Notice;
import com.campus.smart.mapper.AiKnowledgeMapper;
import com.campus.smart.mapper.NoticeMapper;
import com.campus.smart.utils.LocalVectorizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识入库服务：将校园公告等文档切块向量化写入知识库
 *
 * <p>切分策略：按 300-500 字切块，相邻块保留 ~50 字重叠（overlap），
 * 防止关键句被切在边界；标题拼进片段首行，提升检索命中的语义信号。</p>
 *
 * @author 万盛博
 */
@Slf4j
@Service
public class KnowledgeIngestService {

    @Autowired
    private AiKnowledgeMapper knowledgeMapper;

    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private LocalVectorizer vectorizer;

    @Autowired
    private RagRetriever retriever;

    /** 切块参数 */
    private static final int CHUNK_SIZE = 400;
    private static final int OVERLAP = 50;

    /**
     * 从公告表全量构建知识库（幂等：先清空公告来源的旧片段再入库）
     *
     * @return 入库片段数
     */
    public synchronized int ingestFromNotices() {
        // 清掉旧公告片段（重建）
        knowledgeMapper.delete(new LambdaQueryWrapper<AiKnowledge>()
                .eq(AiKnowledge::getSourceType, 0));
        // 拉取公告
        List<Notice> notices = noticeMapper.selectList(
                new LambdaQueryWrapper<Notice>().orderByAsc(Notice::getId));
        int count = 0;
        for (Notice n : notices) {
            String title = n.getTitle() == null ? "" : n.getTitle();
            String content = n.getContent() == null ? "" : n.getContent();
            if (content.isBlank()) {
                continue;
            }
            String full = title + "\n" + content;
            for (String chunk : splitWithOverlap(full)) {
                AiKnowledge k = new AiKnowledge();
                k.setSourceType(0);
                k.setSourceId(n.getId());
                k.setTitle(title);
                k.setContent(chunk);
                k.setEmbeddingModel("local-ngram-256");
                k.setTokenCount(chunk.length());
                k.setEnabled(1);
                knowledgeMapper.insert(k);
                count++;
            }
        }
        retriever.refreshCache();
        log.info("[RAG] 公告知识入库完成：{} 条公告 → {} 个片段", notices.size(), count);
        return count;
    }

    /**
     * 手动添加一条知识（业务规则等）
     */
    public int addKnowledge(String title, String content) {
        int count = 0;
        for (String chunk : splitWithOverlap(title + "\n" + content)) {
            AiKnowledge k = new AiKnowledge();
            k.setSourceType(3);
            k.setTitle(title);
            k.setContent(chunk);
            k.setEmbeddingModel("local-ngram-256");
            k.setTokenCount(chunk.length());
            k.setEnabled(1);
            knowledgeMapper.insert(k);
            count++;
        }
        retriever.refreshCache();
        return count;
    }

    /**
     * 带重叠滑窗切分
     */
    private List<String> splitWithOverlap(String text) {
        List<String> chunks = new ArrayList<>();
        String clean = text.replaceAll("\\s{2,}", " ").trim();
        if (clean.length() <= CHUNK_SIZE) {
            chunks.add(clean);
            return chunks;
        }
        int step = CHUNK_SIZE - OVERLAP;
        for (int i = 0; i < clean.length(); i += step) {
            int end = Math.min(i + CHUNK_SIZE, clean.length());
            chunks.add(clean.substring(i, end));
            if (end == clean.length()) {
                break;
            }
        }
        return chunks;
    }
}
