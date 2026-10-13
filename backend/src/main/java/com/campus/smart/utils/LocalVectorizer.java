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
        // 词频归一化（除以 n-gram 总数）得到归一化向量
        double total = buckets.values().stream().mapToDouble(Double::doubleValue).sum();
        double[] vec = new double[DIM];
        if (total > 0) {
            buckets.forEach((k, v) -> vec[k] = v / total);
        }
        return vec;
    }

    /**
     * 余弦相似度（输入向量已归一化，等价于点积）
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
