package com.competition.energycontrollerplatform.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 令牌工具类。
 * <p>
 * 提供 Token 生成、解析、校验的完整能力。
 * 使用 HMAC-SHA256 签名算法，密钥通过配置文件注入（支持环境变量覆盖）。
 */
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    /**
     * HMAC-SHA256 最小密钥长度 256 bits → Base64 后至少 44 字符。
     * 开发环境使用内置默认值，生产环境通过环境变量 JWT_WEB_SECRET 注入。
     */
    @Value("${jwt.web-secret:${JWT_WEB_SECRET:RW5lcmd5TWFuYWdlbWVudFBsYXRmb3JtU2VjcmV0S2V5Rm9ySldUVG9rZW5HZW5lcmF0aW9uMjAyNg==}}")
    private String secretBase64;

    private SecretKey secretKey;

    /**
     * 延迟初始化密钥，确保 @Value 注入完成后再构建 SecretKey。
     */
    private SecretKey getSecretKey() {
        if (secretKey == null) {
            secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        }
        return secretKey;
    }

    /**
     * JWT 密钥安全校验（profile 无关）。
     * <p>
     * 在 Bean 初始化完成后立即执行，不依赖 {@code spring.profiles.active} 判断。
     * 无论开发还是生产环境，密钥不满足安全要求即拒绝启动。
     * <ul>
     *   <li>密钥为空或长度不足 44 字符 → 拒绝启动</li>
     *   <li>密钥为已知硬编码默认值 → 拒绝启动</li>
     * </ul>
     * 开发环境请在 {@code application.yml} 中设置固定的 dev 密钥。
     *
     * @throws IllegalStateException 密钥不安全时抛出
     */
    @PostConstruct
    public void validateProductionSecret() {
        // 已知的硬编码默认值（Base64 字面量，与 @Value 嵌套默认值一致）
        final String KNOWN_DEFAULT =
                "RW5lcmd5TWFuYWdlbWVudFBsYXRmb3JtU2VjcmV0S2V5Rm9ySldUVG9rZW5HZW5lcmF0aW9uMjAyNg==";

        // ── 校验1：密钥为空或长度不足（HMAC-SHA256 要求 ≥256 bits = 44 Base64 字符） ──
        if (secretBase64 == null || secretBase64.length() < 44) {
            throw new IllegalStateException(
                    "\n============================================================\n"
                    + "❌ 致命错误：JWT Web 密钥未配置或长度不足（至少 44 字符 Base64）！\n"
                    + "   请设置环境变量 JWT_WEB_SECRET。\n"
                    + "   生成命令：openssl rand -base64 32\n"
                    + "   开发环境可在 application.yml 中设置 jwt.web-secret 固定值。\n"
                    + "   应用将拒绝启动。\n"
                    + "============================================================"
            );
        }

        // ── 校验2：密钥为已知硬编码默认值（源码中的 @Value 嵌套默认值） ──
        if (KNOWN_DEFAULT.equals(secretBase64)) {
            throw new IllegalStateException(
                    "\n============================================================\n"
                    + "❌ 致命错误：使用了默认 JWT Web 密钥，存在严重安全风险！\n"
                    + "   请设置环境变量 JWT_WEB_SECRET 覆盖默认值。\n"
                    + "   生成命令：openssl rand -base64 32\n"
                    + "   应用将拒绝启动。\n"
                    + "============================================================"
            );
        }
    }

    /**
     * Token 有效期：24 小时（毫秒）
     */
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000L;

    // ────────────────── 生成 Token ──────────────────

    /**
     * 基于用户信息生成 JWT Token。
     *
     * @param userId   用户主键 ID
     * @param username 用户名
     * @param role     角色编码（ADMIN / OPERATOR / MERCHANT）
     * @return 加密后的 JWT 字符串
     */
    public String generateToken(Long userId, String username, String role, Integer tokenVersion) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .claim("tokenVersion", tokenVersion)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSecretKey())
                .compact();
    }

    // ────────────────── 解析 Token ──────────────────

    /**
     * 从 Token 中提取全部 Claims。
     *
     * @param token JWT 字符串
     * @return 解析后的 Claims 对象
     * @throws ExpiredJwtException      令牌已过期
     * @throws MalformedJwtException    令牌格式错误
     * @throws SignatureException       签名不匹配（被篡改）
     * @throws IllegalArgumentException 令牌为空或非法
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中提取用户 ID。
     */
    public Long getUserIdFromToken(String token) {
        String subject = parseToken(token).getSubject();
        return Long.valueOf(subject);
    }

    /**
     * 从 Token 中提取用户名。
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).get("username", String.class);
    }

    /**
     * 从 Token 中提取角色。
     */
    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }

    /**
     * 从 Token 中提取 tokenVersion。
     */
    public Integer getTokenVersionFromToken(String token) {
        return parseToken(token).get("tokenVersion", Integer.class);
    }

    // ────────────────── 校验 Token ──────────────────

    /**
     * 校验 Token 是否有效（未过期且签名正确）。
     *
     * @return true-有效，false-无效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT 已过期: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT 格式错误: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT 签名无效: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 参数非法: {}", e.getMessage());
        }
        return false;
    }
}
