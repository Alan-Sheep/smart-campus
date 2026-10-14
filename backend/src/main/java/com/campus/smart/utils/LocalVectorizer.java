package com.campus.smart.utils;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量本地向量化器（字符 n-gram hash 方案）
 *
 * <p>设计考量：知识库片段规模在万级以内时，无需调用远程 Embedding API——
 * 使用字符 2/3-gram 的哈希映射到固定维度，配合词频归一化即可有效表征中文文本语义，
 * 检索在内存中完成，零 API 成本、零外部依赖。
 * 当数据规模增长到十万级以上，可平滑替换为 Embedding API + 专业向量库，
 * 本类的对外接口（textToVector / cosineSimilarity）保持不变。</p>
 *
 * @author 万盛博
 */
@Component
public class LocalVectorizer {

    /** 向量维度 */
    private static final int DIM = 256;

    /** n-gram 窗口：2字词 + 3字词混合，兼顾中文分词粒度 */
    private static final int[] NGRAM_SIZES = {2, 3};

    /**
     * 文本转向量（TF 归一化）
     *
     * @param text 输入文本
     * @return 归一化后的稠密向量
     */
    public double[] textToVector(String text) {
        if (text == null || text.isBlank()) {
            return new double[DIM];
        }
        // 预处理：去空白，统一小写
        String clean = text.replaceAll("\\s+", "").toLowerCase();
        Map<Integer, Double> buckets = new HashMap<>();
        // 统计各 n-gram 落桶的词频
        for (int n : NGRAM_SIZES) {
            for (int i = 0; i + n <= clean.length(); i++) {
                String gram = clean.substring(i, i + n);
                int bucket = Math.floorMod(gram.hashCode(), DIM);
                buckets.merge(bucket, 1.0, Double::sum);
            }
        }
        // L2 归一化：使向量模长为 1，这样点积才等于余弦相似度
        // （早期版本误用 L1 归一化"除以总和"，导致相同文本的相似度只有 0.077、
        //   且长文本得分天然偏低——由单元测试 LocalVectorizerTest 发现并修正）
        double norm = Math.sqrt(buckets.values().stream()
                .mapToDouble(v -> v * v).sum());
        double[] vec = new double[DIM];
        if (norm > 0) {
            buckets.forEach((k, v) -> vec[k] = v / norm);
        }
        return vec;
    }

    /**
     * 余弦相似度
     *
     * <p>前提：输入向量已做 <b>L2 归一化</b>（模长为 1），此时余弦相似度等价于点积，
     * 取值范围 0~1——相同文本为 1.0，完全不相关趋近 0。</p>
     */
    public double cosineSimilarity(double[] a, double[] b) {
        double dot = 0.0;
        for (int i = 0; i < DIM && i < a.length && i < b.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    /**
     * 批量转向量
     */
    public List<double[]> textToVectors(List<String> texts) {
        List<double[]> list = new ArrayList<>(texts.size());
        for (String t : texts) {
            list.add(textToVector(t));
        }
        return list;
    }
}
