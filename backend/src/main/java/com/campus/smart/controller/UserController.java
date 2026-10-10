package com.campus.smart.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.smart.common.Result;
import com.campus.smart.entity.User;
import com.campus.smart.service.EmailCodeService;
import com.campus.smart.service.UserService;
import com.campus.smart.utils.JwtUtils;
import com.campus.smart.utils.RedisUtils;
import com.campus.smart.utils.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 用户模块控制器
 */
@Tag(name = "用户模块")
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private EmailCodeService emailCodeService;

    /**
     * Jackson 对象映射器（Spring Bean，已注册 JavaTimeModule 支持 LocalDateTime）
     */
    private final ObjectMapper objectMapper = com.campus.smart.config.RedisConfig.buildObjectMapper();

    /**
     * 发送短信验证码
     * 生成6位随机验证码，存入 Redis，过期时间5分钟
     * 模拟场景：真实项目中调用短信服务商接口发送短信，这里直接返回验证码方便测试
     *
     * @param phone 手机号
     * @return 验证码（模拟，实际项目不返回）
     */
    @Operation(summary = "发送短信验证码")
    @GetMapping("/sendCode")
    public Result<String> sendCode(@RequestParam String phone) {
        // 参数校验
        if (phone == null || phone.isEmpty()) {
            return Result.error("手机号不能为空");
        }
        // 简单手机号格式校验
        if (phone.length() != 11) {
            return Result.error("手机号格式不正确");
        }

        try {
            // 生成6位随机验证码
            Random random = new Random();
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                code.append(random.nextInt(10));
            }
            String verifyCode = code.toString();

            // Redis key 格式：campus:sms:code:{phone}
            String redisKey = "campus:sms:code:" + phone;

            // 存入 Redis，过期时间 5 分钟（300秒）
            boolean success = redisUtils.setWithExpire(redisKey, verifyCode, 300);

            if (success) {
                // 模拟：返回验证码方便测试，真实项目不返回验证码，只提示发送成功
                return Result.success(verifyCode);
            } else {
                // Redis 写入失败，降级处理：仍然返回验证码（实际项目可以走备用通道）
                return Result.success(verifyCode);
            }
        } catch (Exception e) {
            // 异常处理，不影响主业务
            System.err.println("发送短信验证码异常: " + e.getMessage());
            return Result.error("发送验证码失败，请稍后重试");
        }
    }

    /**
     * 发送邮箱验证码
     *
     * <p>替代手机短信的登录验证方式（个人主体无法完成运营商短信签名报备）。</p>
     *
     * <p>安全设计（比原短信接口强的地方，面试可讲）：</p>
     * <ul>
     *   <li>同一邮箱 60 秒重发间隔（Redis TTL 标记，后端强校验，不依赖前端倒计时）</li>
     *   <li>同一邮箱单日上限 10 条、同一 IP 单日上限 30 条（防换邮箱刷）</li>
     *   <li>错误次数超限锁定 30 分钟（防暴力猜码）</li>
     *   <li>真实发送模式下验证码绝不返回给前端；开发模式(mock)才返回，便于联调</li>
     * </ul>
     */
    @Operation(summary = "发送邮箱验证码")
    @GetMapping("/sendEmailCode")
    public Result<Map<String, Object>> sendEmailCode(@RequestParam String email, HttpServletRequest request) {
        String ip = resolveIp(request);
        try {
            Map<String, Object> data = emailCodeService.sendCode(email, ip);
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (IllegalStateException e) {
            // 限流/锁定类错误：明确告知用户原因
            return Result.error(e.getMessage());
        } catch (Exception e) {
            // 邮件发送失败等异常：不暴露内部细节
            System.err.println("发送邮箱验证码异常: " + e.getMessage());
            return Result.error("验证码发送失败，请稍后重试");
        }
    }

    /**
     * 获取客户端真实 IP（支持代理链）
     */
    private String resolveIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP"};
        for (String h : headers) {
            String v = request.getHeader(h);
            if (v != null && !v.isEmpty() && !"unknown".equalsIgnoreCase(v)) {
                // X-Forwarded-For 可能是逗号分隔的链，第一个是原始客户端
                int comma = v.indexOf(',');
                return comma > 0 ? v.substring(0, comma).trim() : v.trim();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * 用户登录
     * 支持三种方式：邮箱验证码登录 / 手机号验证码登录 / 微信登录
     *
     * <p>安全修复（原漏洞）：旧逻辑在 code 参数为空时跳过验证码校验直接登录，
     * 等于验证码形同虚设（任意手机号/邮箱都能登录）。现改为：
     * 手机号/邮箱登录【必须】携带验证码，否则拒绝。</p>
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String phone = params.get("phone");
        String email = params.get("email");
        String wxCode = params.get("wxCode");
        String code = params.get("code"); // 验证码（邮箱或手机号）

        User user;

        // 邮箱验证码登录
        if (email != null && !email.isEmpty()) {
            if (code == null || code.isEmpty()) {
                // 堵住旧漏洞：验证码必传
                return Result.error("请输入验证码");
            }
            String verifyErr = emailCodeService.verify(email, code);
            if (verifyErr != null) {
                return Result.error(verifyErr);
            }

            // 按邮箱查用户（没有则自动注册）
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, email.trim().toLowerCase());
            user = userService.getOne(wrapper);
            if (user == null) {
                user = new User();
                user.setEmail(email.trim().toLowerCase());
                // 用邮箱前缀做昵称，比"校园用户xxxx"更自然
                String prefix = email.split("@")[0];
                user.setNickname("同学" + prefix.substring(0, Math.min(6, prefix.length())));
                user.setAvatar("https://thirdwx.qlogo.cn/mmopen/vi_32/POgEwh4mIHO4nibH0KlMECNjjGxQUq24ZEaGT4poC6icRiccVGKSyXwibcPq4BWmiaIGuG1icwxaQX6grC9VemZoJ8rg/132");
                userService.save(user);
            }
        }
        // 手机号验证码登录
        else if (phone != null && !phone.isEmpty()) {
            // 安全校验：验证码必传（原漏洞在此处被绕过）
            if (code == null || code.isEmpty()) {
                return Result.error("请输入验证码");
            }
            String redisKey = "campus:sms:code:" + phone;
            String cachedCode = null;
            try {
                // 从 Redis 中读取验证码
                Object codeObj = redisUtils.get(redisKey);
                if (codeObj != null) {
                    cachedCode = String.valueOf(codeObj);
                }
            } catch (Exception e) {
                System.err.println("读取验证码缓存异常: " + e.getMessage());
            }

            // 校验验证码
            if (cachedCode == null) {
                return Result.error("验证码已过期或不存在");
            }
            if (!cachedCode.equals(code)) {
                return Result.error("验证码错误");
            }

            // 校验通过后删除验证码（一次性使用）
            try {
                redisUtils.delete(redisKey);
            } catch (Exception e) {
                System.err.println("删除验证码缓存异常: " + e.getMessage());
            }

            // 查询用户
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, phone);
            user = userService.getOne(wrapper);
            // 如果用户不存在，创建新用户
            if (user == null) {
                user = new User();
                user.setPhone(phone);
                user.setNickname("校园用户" + phone.substring(phone.length() - 4));
                user.setAvatar("https://thirdwx.qlogo.cn/mmopen/vi_32/POgEwh4mIHO4nibH0KlMECNjjGxQUq24ZEaGT4poC6icRiccVGKSyXwibcPq4BWmiaIGuG1icwxaQX6grC9VemZoJ8rg/132");
                userService.save(user);
            }
        }
        // 微信登录
        else if (wxCode != null && !wxCode.isEmpty()) {
            // 模拟微信登录，用code当openid
            String openid = "wx_" + wxCode;
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getOpenid, openid);
            user = userService.getOne(wrapper);
            // 如果用户不存在，创建新用户
            if (user == null) {
                user = new User();
                user.setOpenid(openid);
                user.setNickname("微信用户" + UUID.randomUUID().toString().substring(0, 4));
                user.setAvatar("https://thirdwx.qlogo.cn/mmopen/vi_32/POgEwh4mIHO4nibH0KlMECNjjGxQUq24ZEaGT4poC6icRiccVGKSyXwibcPq4BWmiaIGuG1icwxaQX6grC9VemZoJ8rg/132");
                userService.save(user);
            }
        } else {
            return Result.error("请传入手机号或微信code");
        }

        // 生成JWT token
        String token = jwtUtils.generateToken(user.getId(), user.getOpenid());

        // 登录成功后，清除用户信息缓存（保证下次获取时是最新的）
        try {
            String userCacheKey = "campus:user:info:" + user.getId();
            redisUtils.delete(userCacheKey);
        } catch (Exception e) {
            System.err.println("清除用户信息缓存异常: " + e.getMessage());
        }

        // 返回token和用户信息
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", user);
        return Result.success(result);
    }

    /**
     * 获取当前用户信息
     * 加入 Redis 缓存：先查缓存，没有查数据库后写入缓存（30分钟过期）
     */
    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public Result<User> getUserInfo() {
        Long userId = UserContext.getUserId();
        String cacheKey = "campus:user:info:" + userId;

        User user = null;
        try {
            // 先从 Redis 缓存中读取
            Object cacheObj = redisUtils.get(cacheKey);
            if (cacheObj != null) {
                // 缓存命中，转换为 User 对象
                user = objectMapper.convertValue(cacheObj, User.class);
            }
        } catch (Exception e) {
            // Redis 读取失败，打印日志，继续走数据库查询
            System.err.println("用户信息缓存读取异常: " + e.getMessage());
        }

        // 缓存未命中，查询数据库
        if (user == null) {
            user = userService.getById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            // 写入 Redis 缓存，过期时间 30 分钟（1800秒）
            try {
                redisUtils.setWithExpire(cacheKey, user, 1800);
            } catch (Exception e) {
                System.err.println("用户信息缓存写入异常: " + e.getMessage());
            }
        }

        return Result.success(user);
    }

    /**
     * 更新用户信息
     * 更新后清除用户信息缓存
     */
    @Operation(summary = "更新用户信息")
    @PutMapping("/info")
    public Result<User> updateUserInfo(@RequestBody User user) {
        Long userId = UserContext.getUserId();
        user.setId(userId);
        // 只允许更新 nickname, avatar, phone, schoolId
        User updateUser = new User();
        updateUser.setId(userId);
        if (user.getNickname() != null) {
            updateUser.setNickname(user.getNickname());
        }
        if (user.getAvatar() != null) {
            updateUser.setAvatar(user.getAvatar());
        }
        if (user.getPhone() != null) {
            updateUser.setPhone(user.getPhone());
        }
        // 学校归属：注册（首次登录）时用户没有选择学校的入口，school_id 会是 NULL，
        // 导致发布的订单/帖子缺少学校归属。这里放开 schoolId 的更新，
        // 供前端在登录后引导用户选择学校时回填。
        if (user.getSchoolId() != null) {
            updateUser.setSchoolId(user.getSchoolId());
        }
        userService.updateById(updateUser);

        User updatedUser = userService.getById(userId);

        // 更新成功后，清除用户信息缓存（保证下次获取时是最新的）
        try {
            String cacheKey = "campus:user:info:" + userId;
            redisUtils.delete(cacheKey);
        } catch (Exception e) {
            System.err.println("清除用户信息缓存异常: " + e.getMessage());
        }

        return Result.success(updatedUser);
    }

    /**
     * 退出登录
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        Long userId = UserContext.getUserId();

        // 退出登录时清除用户信息缓存
        try {
            if (userId != null) {
                String cacheKey = "campus:user:info:" + userId;
                redisUtils.delete(cacheKey);
            }
        } catch (Exception e) {
            System.err.println("退出登录清除缓存异常: " + e.getMessage());
        }

        // 清除当前线程的用户ID
        UserContext.remove();
        return Result.success();
    }
}
