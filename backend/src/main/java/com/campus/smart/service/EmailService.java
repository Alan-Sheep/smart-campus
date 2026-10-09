package com.campus.smart.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 邮件发送服务
 *
 * <p>策略说明：本类是「真实发送」实现，基于 Spring Mail（SMTP）。
 * 当 email-code.enabled=false（未配置邮箱账号）时，调用方 EmailCodeService
 * 走开发模式（验证码不真正发送，直接返回给前端），不依赖本类。
 * 两个实现的切换由配置驱动，与策略模式的 SmsService 抽象等价——
 * 面试可讲：基础设施层的多环境实现切换用配置驱动而非 if-else 散落业务。</p>
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    /** 发件人账号（与 SMTP 认证账号一致，QQ 邮箱强制要求） */
    @Value("${spring.mail.username:}")
    private String fromUser;

    /** 发件人显示名 */
    @Value("${email-code.from-name:智慧校园}")
    private String fromName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 是否具备真实发送条件（账号已配置）
     */
    public boolean isConfigured() {
        return fromUser != null && !fromUser.isEmpty();
    }

    /**
     * 发送验证码邮件
     *
     * @param to   收件邮箱
     * @param code 验证码
     * @param ttlSeconds 有效期（秒），用于邮件文案
     * @throws Exception 发送失败（由调用方决定降级策略）
     */
    public void sendVerifyCode(String to, String code, int ttlSeconds) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        String expireTime = LocalDateTime.now().plusSeconds(ttlSeconds)
                .format(DateTimeFormatter.ofPattern("HH:mm"));

        // 简洁 HTML 正文（小程序/移动端邮件场景，不需要复杂样式）
        String html = "<div style=\"max-width:420px;margin:0 auto;padding:24px;"
                + "font-family:-apple-system,'PingFang SC','Microsoft YaHei',sans-serif;\">"
                + "<h2 style=\"color:#6c4ee6;margin:0 0 12px;\">智慧校园</h2>"
                + "<p style=\"color:#333;font-size:14px;\">你正在登录智慧校园，验证码为：</p>"
                + "<p style=\"font-size:32px;font-weight:700;letter-spacing:8px;"
                + "color:#6c4ee6;margin:16px 0;\">" + code + "</p>"
                + "<p style=\"color:#999;font-size:13px;\">验证码 " + ttlSeconds / 60
                + " 分钟内有效（今天 " + expireTime + " 前有效），请勿泄露给他人。</p>"
                + "<p style=\"color:#bbb;font-size:12px;margin-top:24px;\">如非本人操作，请忽略本邮件。</p>"
                + "</div>";

        helper.setFrom(fromUser, fromName);
        helper.setTo(to);
        helper.setSubject("【智慧校园】登录验证码：" + code);
        helper.setText(html, true);

        mailSender.send(message);
    }
}
