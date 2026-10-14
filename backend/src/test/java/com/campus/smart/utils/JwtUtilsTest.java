package com.campus.smart.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT 工具类单元测试
 *
 * <p>认证是整个系统的入口防线，token 一旦能被伪造或篡改绕过，后续所有权限校验都形同虚设。
 * 这里重点覆盖：正确签发与解析、角色区分、篡改防护、过期识别。</p>
 *
 * @author 万盛博
 */
@DisplayName("JwtUtils · Token 签发与解析")
class JwtUtilsTest {

    private static final String TEST_SECRET = "UnitTest-Secret-Key-For-JwtUtils-Verification";
    private static final long ONE_DAY = 86400000L;

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        // @Value 字段在单元测试中通过反射注入（不启动 Spring 上下文）
        ReflectionTestUtils.setField(jwtUtils, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "expiration", ONE_DAY);
    }

    @Test
    @DisplayName("用户 token 可解析出正确的 userId")
    void shouldParseUserIdFromToken() {
        String token = jwtUtils.generateToken(39L, "test-openid-123");
        assertNotNull(token);
        assertEquals(Long.valueOf(39L), jwtUtils.getUserId(token));
    }

    @Test
    @DisplayName("用户 token 的角色为 user")
    void userTokenShouldHaveUserRole() {
        String token = jwtUtils.generateToken(39L, "openid");
        assertEquals("user", jwtUtils.getRole(token));
    }

    @Test
    @DisplayName("管理员 token 可解析出 adminId 且角色为 admin（与用户 token 隔离）")
    void adminTokenShouldBeDistinguishable() {
        String token = jwtUtils.generateAdminToken(1L, "admin");
        assertEquals(Long.valueOf(1L), jwtUtils.getAdminId(token));
        assertEquals("admin", jwtUtils.getRole(token));
    }

    @Test
    @DisplayName("被篡改的 token 解析必须抛异常（防伪造）")
    void tamperedTokenShouldThrow() {
        String token = jwtUtils.generateToken(39L, "openid");
        // 改动签名段最后几个字符，模拟篡改
        String tampered = token.substring(0, token.length() - 3) + "xyz";
        assertThrows(Exception.class, () -> jwtUtils.parseToken(tampered));
    }

    @Test
    @DisplayName("使用错误密钥签发的 token 无法解析（密钥隔离）")
    void tokenSignedWithWrongSecretShouldFail() {
        String token = jwtUtils.generateToken(39L, "openid");

        JwtUtils another = new JwtUtils();
        ReflectionTestUtils.setField(another, "secret", "Another-Different-Secret-Key-For-Test");
        ReflectionTestUtils.setField(another, "expiration", ONE_DAY);

        assertThrows(Exception.class, () -> another.parseToken(token));
    }

    @Test
    @DisplayName("过期 token 被识别为已过期且不抛异常")
    void expiredTokenShouldBeDetected() {
        // 构造一个签发即过期的 token
        ReflectionTestUtils.setField(jwtUtils, "expiration", -1000L);
        String expired = jwtUtils.generateToken(39L, "openid");

        assertTrue(jwtUtils.isExpired(expired), "过期 token 应被识别为已过期");
    }

    @Test
    @DisplayName("有效 token 不应被误判为过期")
    void validTokenShouldNotBeExpired() {
        String token = jwtUtils.generateToken(39L, "openid");
        assertFalse(jwtUtils.isExpired(token));
    }

    @Test
    @DisplayName("非法字符串作为 token 时判为过期（异常被吞掉，不向外抛）")
    void malformedTokenShouldBeTreatedAsExpired() {
        assertTrue(jwtUtils.isExpired("not-a-valid-jwt-token"));
    }
}
