package com.campus.smart.service;

import com.campus.smart.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.time.LocalDate;

/**
 * 邮箱验证码服务
 *
 * <p>完整生命周期：</p>
 * <pre>
 * 发送：频率限制 → 生成6位码 → 存Redis(TTL) → (启用时)邮件发送
 * 校验：错误次数检查 → 比对 → 成功删除 / 失败计数（超限锁定）
 * </pre>
 *
 * <p>Redis Key 设计（面试可讲：把"限流"和"凭证"分开管理，各 Key 独立 TTL）：</p>
 * <pre>
 * campus:email:code:{email}       验证码本体，TTL=有效期
 * campus:email:interval:{email}   重发间隔标记，TTL=60s（60秒内不许重发）
 * campus:email:day:{email}:{date} 单日发送计数，TTL=到当天结束
 * campus:email:ip:{ip}:{date}     同 IP 单日发送计数
 * campus:email:fail:{email}       验证错误次数
 * campus:email:lock:{email}       锁定标记（错误超限后设置）
 * </pre>
 */
@Service
public class EmailCodeService {

    private final RedisUtils redisUtils;
    private final EmailService emailService;

    public EmailCodeService(RedisUtils redisUtils, EmailService emailService) {
        this.redisUtils = redisUtils;
        this.emailService = emailService;
    }

    @Value("${email-code.enabled:false}")
    private boolean enabled;

    @Value("${email-code.ttl-seconds:300}")
    private int ttlSeconds;

    @Value("${email-code.resend-interval:60}")
    private int resendInterval;

    @Value("${email-code.daily-limit:10}")
    private int dailyLimit;

    @Value("${email-code.ip-daily-limit:30}")
    private int ipDailyLimit;

    @Value("${email-code.max-fail-count:5}")
    private int maxFailCount;

    @Value("${email-code.lock-seconds:1800}")
    private int lockSeconds;

    /** 邮箱格式 */
    private static final String EMAIL_REGEX = "^[1-9]\\d{4,10}@qq\\.com$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    /**
     * 发送验证码
     *
     * @param email 收件邮箱
     * @param ip    请求方 IP（用于 IP 限流）
     * @return (code, mock)：mock=true 表示开发模式（验证码直接返回前端）
     * @throws IllegalStateException 触发任何限流/锁定
     * @throws Exception             邮件发送失败
     */
    public Map<String, Object> sendCode(String email, String ip) throws Exception {
        // ---- 基础校验 ----
        if (email == null || !email.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        email = email.trim().toLowerCase();

        // ---- 防刷1：错误锁定中的邮箱不允许再获取（重发也拦） ----
        String lockKey = "campus:email:lock:" + email;
        if (Boolean.TRUE.equals(redisUtils.hasKey(lockKey))) {
            long ttl = redisUtils.getExpire(lockKey);
            throw new IllegalStateException("错误次数过多已锁定，请 " + Math.max(ttl / 60, 1) + " 分钟后再试");
        }

        // ---- 防刷2：重发间隔 ----
        String intervalKey = "campus:email:interval:" + email;
        if (Boolean.TRUE.equals(redisUtils.hasKey(intervalKey))) {
            throw new IllegalStateException("发送过于频繁，请 " + redisUtils.getExpire(intervalKey) + " 秒后再试");
        }

        // ---- 防刷3：单邮箱单日上限 ----
        String today = LocalDate.now().toString();
        String dayKey = "campus:email:day:" + email + ":" + today;
        Long sent = toLong(redisUtils.get(dayKey));
        if (sent != null && sent >= dailyLimit) {
            throw new IllegalStateException("今日验证码发送次数已达上限，请明天再试");
        }

        // ---- 防刷4：同 IP 单日上限 ----
        if (ip != null && !ip.isEmpty()) {
            String ipKey = "campus:email:ip:" + ip + ":" + today;
            Long ipSent = toLong(redisUtils.get(ipKey));
            if (ipSent != null && ipSent >= ipDailyLimit) {
                throw new IllegalStateException("操作过于频繁，请明天再试");
            }
        }

        // ---- 生成验证码（SecureRandom 避免可预测） ----
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        String code = sb.toString();

        // ---- 存 Redis ----
        String codeKey = "campus:email:code:" + email;
        redisUtils.setWithExpire(codeKey, code, ttlSeconds);
        // 清空错误计数（新码新机会）
        redisUtils.delete("campus:email:fail:" + email);

        // ---- 发送 ----
        boolean mock = true;
        if (enabled && emailService.isConfigured()) {
            emailService.sendVerifyCode(email, code, ttlSeconds);
            mock = false; // 真实发送：验证码不出现在响应里
        } else if (enabled && !emailService.isConfigured()) {
            // 配置开了但账号没填：明确报错，避免"以为发了其实没发"
            throw new IllegalStateException("邮件服务未配置完整，请联系管理员或稍后再试");
        }
        // enabled=false：开发模式，mock=true，验证码随响应返回给前端弹窗显示

        // ---- 记数与间隔（发送成功才计） ----
        redisUtils.setWithExpire(intervalKey, "1", resendInterval);
        long dayNew = (sent == null ? 0 : sent) + 1;
        // TTL 设到当天剩余秒数（上限 24h），次日自然重新计数
        long dayRemain = secondsUntilMidnight();
        redisUtils.setWithExpire(dayKey, dayNew, dayRemain);
        if (ip != null && !ip.isEmpty()) {
            String ipKey = "campus:email:ip:" + ip + ":" + today;
            Long ipSent = toLong(redisUtils.get(ipKey));
            redisUtils.setWithExpire(ipKey, (ipSent == null ? 0 : ipSent) + 1, dayRemain);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("mock", mock);
        if (mock) {
            result.put("code", code); // 开发模式才返回
        }
        return result;
    }

    /**
     * 校验验证码
     *
     * @param email 邮箱
     * @param code  用户输入
     * @return null=校验通过；非 null=错误信息
     */
    public String verify(String email, String code) {
        if (email == null || email.isEmpty() || code == null || code.isEmpty()) {
            return "请填写邮箱和验证码";
        }
        email = email.trim().toLowerCase();

        String codeKey = "campus:email:code:" + email;
        String failKey = "campus:email:fail:" + email;
        String lockKey = "campus:email:lock:" + email;

        // 锁定检查
        if (Boolean.TRUE.equals(redisUtils.hasKey(lockKey))) {
            long ttl = redisUtils.getExpire(lockKey);
            return "错误次数过多已锁定，请 " + Math.max(ttl / 60, 1) + " 分钟后再试";
        }

        Object codeObj = redisUtils.get(codeKey);
        if (codeObj == null) {
            return "验证码已过期，请重新获取";
        }

        if (!String.valueOf(codeObj).equals(code.trim())) {
            // 错误计数
            Long fails = toLong(redisUtils.get(failKey));
            long newFails = (fails == null ? 0 : fails) + 1;
            if (newFails >= maxFailCount) {
                // 锁定并清掉验证码，防止继续试
                redisUtils.setWithExpire(lockKey, "1", lockSeconds);
                redisUtils.delete(codeKey);
                redisUtils.delete(failKey);
                return "错误次数过多已锁定，请 " + (lockSeconds / 60) + " 分钟后再试";
            }
            redisUtils.setWithExpire(failKey, newFails, ttlSeconds);
            return "验证码错误，剩余机会 " + (maxFailCount - newFails) + " 次";
        }

        // 一次性消费：校验通过立即删除
        redisUtils.delete(codeKey);
        redisUtils.delete(failKey);
        return null;
    }

    /** 开发模式下查询验证码（供 sendCode 接口在 mock 时返回；不落库不泄露生产） */
    public String peekCode(String email) {
        Object o = redisUtils.get("campus:email:code:" + email.trim().toLowerCase());
        return o == null ? null : String.valueOf(o);
    }

    /** 当前配置是否为开发模式 */
    public boolean isMockMode() {
        return !(enabled && emailService.isConfigured());
    }

    // ---------- helpers ----------
    private Long toLong(Object o) {
        if (o == null) return null;
        try { return Long.valueOf(String.valueOf(o)); } catch (NumberFormatException e) { return null; }
    }

    private long secondsUntilMidnight() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime mid = now.toLocalDate().plusDays(1).atStartOfDay();
        return Math.max(60, java.time.Duration.between(now, mid).getSeconds());
    }
}
