package com.campus.smart.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * DeepSeek 大模型调用封装（OpenAI 兼容协议）
 *
 * <p>支持两种模式：
 * 1. 同步调用（{@link #chat}）：一次性返回完整回答，用于内部调用与降级兜底；
 * 2. SSE 流式调用（{@link #chatStream}）：token 逐段推送到前端，打字机体验，
 *    大幅降低长回答的体感等待时间。</p>
 *
 * @author 万盛博
 */
@Slf4j
@Service
public class DeepSeekService {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${deepseek.chat-model:deepseek-chat}")
    private String chatModel;

    @Value("${deepseek.timeout-seconds:30}")
    private int timeoutSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 消息结构（role: system/user/assistant） */
    public static class ChatMessage {
        private final String role;
        private final String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String role() { return role; }

        public String content() { return content; }
    }

    /**
     * 同步对话（非流式）
     *
     * @param messages 对话消息列表（含 system 提示词）
     * @return 模型完整回答
     */
    public String chat(List<ChatMessage> messages) throws Exception {
        String payload = buildPayload(messages, false);
        HttpURLConnection conn = openConnection("/chat/completions", payload);
        int code = conn.getResponseCode();
        if (code != 200) {
            String err = readError(conn);
            log.error("[DeepSeek] 调用失败 HTTP {}: {}", code, err);
            throw new RuntimeException("大模型接口异常(" + code + ")");
        }
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        JsonNode root = objectMapper.readTree(body.toString());
        return root.path("choices").path(0).path("message").path("content").asText("");
    }

    /**
     * SSE 流式对话：token 逐段推给前端（打字机效果）
     *
     * @param messages 对话消息列表
     * @param emitter  Spring SSE 发射器，前端 EventSource 订阅
     */
    public void chatStream(List<ChatMessage> messages, SseEmitter emitter) {
        // SSE 流式在新线程执行，避免占用容器请求线程
        Thread thread = new Thread(() -> {
            try {
                String payload = buildPayload(messages, true);
                HttpURLConnection conn = openConnection("/chat/completions", payload);
                int code = conn.getResponseCode();
                if (code != 200) {
                    String err = readError(conn);
                    log.error("[DeepSeek] 流式调用失败 HTTP {}: {}", code, err);
                    emitter.send(SseEmitter.event().name("error").data("大模型接口异常，请稍后再试"));
                    emitter.complete();
                    return;
                }
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    StringBuilder full = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        // SSE 数据行格式：data: {...}；data: [DONE] 表示结束
                        if (!line.startsWith("data:")) {
                            continue;
                        }
                        String data = line.substring(5).trim();
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        JsonNode chunk = objectMapper.readTree(data);
                        String delta = chunk.path("choices").path(0).path("delta").path("content").asText("");
                        if (!delta.isEmpty()) {
                            full.append(delta);
                            emitter.send(SseEmitter.event().name("message").data(delta));
                        }
                    }
                    // 流结束：把完整回答作为最后一个事件发给前端（用于落库/展示完整性校验）
                    emitter.send(SseEmitter.event().name("done").data(full.toString()));
                    emitter.complete();
                }
            } catch (Exception e) {
                log.error("[DeepSeek] 流式调用异常", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("AI 助手开小差了，请稍后再试"));
                    emitter.complete();
                } catch (Exception ignore) {
                    // 连接已断开，无需处理
                }
            }
        }, "deepseek-sse");
        thread.setDaemon(true);
        thread.start();
    }

    /** 组装请求体 */
    private String buildPayload(List<ChatMessage> messages, boolean stream) throws Exception {
        List<ChatMessage> all = new ArrayList<>();
        // 系统提示词：角色设定 + 回答约束（抑制幻觉的第一道防线）
        all.add(0, new ChatMessage("system",
                "你是智慧校园平台的学生助手，名字叫\"小智\"。"
                + "回答规则：1.仅依据用户消息中提供的【参考资料】回答，资料中没有的信息明确告知"
                + "\"这个问题我暂时回答不了，建议咨询学校相关部门\"，禁止编造；"
                + "2.回答简洁友好，使用中文；3.涉及具体数据（价格/时间/地点）必须与资料一致。"));
        all.addAll(messages);
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(chatModel).append("\",\"messages\":[");
        for (int i = 0; i < all.size(); i++) {
            ChatMessage m = all.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"role\":\"").append(m.role())
              .append("\",\"content\":\"").append(escape(m.content())).append("\"}");
        }
        sb.append("],\"stream\":").append(stream)
          .append(",\"temperature\":0.3,\"max_tokens\":1024}");
        return sb.toString();
    }

    /** 建立连接 */
    private HttpURLConnection openConnection(String path, String payload) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(timeoutSeconds * 1000);
        conn.setReadTimeout(timeoutSeconds * 1000);
        conn.setDoOutput(true);
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }
        return conn;
    }

    /** 读错误响应体 */
    private String readError(HttpURLConnection conn) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            return "unreadable";
        }
    }

    /** JSON 字符串转义 */
    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
