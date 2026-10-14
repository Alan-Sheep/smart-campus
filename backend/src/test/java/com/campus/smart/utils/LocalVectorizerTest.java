package com.campus.smart.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 轻量向量化器单元测试
 *
 * <p>覆盖：向量维度、相似度语义正确性、边界输入。
 * 这些是 RAG 检索效果的地基——检索不准往往是向量化出了问题。</p>
 *
 * @author 万盛博
 */
@DisplayName("LocalVectorizer · 向量化与相似度")
class LocalVectorizerTest {

    private final LocalVectorizer vectorizer = new LocalVectorizer();

    private static final int EXPECTED_DIM = 256;

    @Test
    @DisplayName("向量维度固定为 256（与检索层约定一致）")
    void shouldProduceFixedDimension() {
        double[] vector = vectorizer.textToVector("校园快递代取服务");
        assertEquals(EXPECTED_DIM, vector.length);
    }

    @Test
    @DisplayName("相同文本的相似度为 1（向量已归一化）")
    void sameTextShouldHaveMaxSimilarity() {
        double[] a = vectorizer.textToVector("如何上架二手商品");
        double[] b = vectorizer.textToVector("如何上架二手商品");
        assertEquals(1.0, vectorizer.cosineSimilarity(a, b), 1e-6);
    }

    @Test
    @DisplayName("相关文本得分高于无关文本（检索有效性的核心前提）")
    void relatedTextShouldScoreHigherThanUnrelated() {
        double[] query = vectorizer.textToVector("快递怎么取");
        double[] related = vectorizer.textToVector("校园快递代取服务上线，配送到宿舍楼下");
        double[] unrelated = vectorizer.textToVector("图书馆开放时间调整通知");

        double relatedScore = vectorizer.cosineSimilarity(query, related);
        double unrelatedScore = vectorizer.cosineSimilarity(query, unrelated);

        assertTrue(relatedScore > unrelatedScore,
                "相关文本得分应更高，实际: related=" + relatedScore + ", unrelated=" + unrelatedScore);
    }

    @Test
    @DisplayName("相似度落在 0~1 区间（n-gram 方案的实际取值范围）")
    void similarityShouldBeInRange() {
        double[] a = vectorizer.textToVector("打印服务怎么收费");
        double[] b = vectorizer.textToVector("打印服务价格说明");
        double score = vectorizer.cosineSimilarity(a, b);
        assertTrue(score >= 0.0 && score <= 1.0, "相似度越界: " + score);
    }

    @Test
    @DisplayName("空文本 / null 返回零向量且不抛异常")
    void emptyAndNullTextShouldBeSafe() {
        assertDoesNotThrow(() -> vectorizer.textToVector(""));
        assertDoesNotThrow(() -> vectorizer.textToVector(null));
        double[] empty = vectorizer.textToVector("");
        assertEquals(EXPECTED_DIM, empty.length);
        for (double v : empty) {
            assertEquals(0.0, v, 1e-9);
        }
    }

    @Test
    @DisplayName("批量向量化返回数量与输入一致")
    void batchVectorizeShouldKeepSize() {
        List<double[]> vectors = vectorizer.textToVectors(
                Arrays.asList("快递代取", "打印服务", "校园跑腿"));
        assertEquals(3, vectors.size());
        for (double[] v : vectors) {
            assertEquals(EXPECTED_DIM, v.length);
        }
    }

    @Test
    @DisplayName("空白字符与大小写不影响向量结果（预处理一致性）")
    void preprocessingShouldBeConsistent() {
        double[] a = vectorizer.textToVector("快递代取");
        double[] b = vectorizer.textToVector(" 快递 代取 ");
        assertEquals(1.0, vectorizer.cosineSimilarity(a, b), 1e-6);
    }
}
