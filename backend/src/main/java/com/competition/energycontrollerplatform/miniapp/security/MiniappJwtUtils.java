package com.competition.energycontrollerplatform.miniapp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
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

/**
 * 小程序专用 JWT 工具类。
 * <p>
 * 与管理员 JWT 使用不同的签名密钥和 claims 结构。
 * Claims: sub=miniAppUserId, customerId, loginType(WECHAT/PHONE)
 * 有效期: 30 天（满足小程序长期登录需求）
 * 密钥通过配置文件注入（支持环境变量覆盖）。
 */
@Component
@RequiredArgsConstructor
public class MiniappJwtUtils {

    private static final Logger log = LoggerFactory.getLogger(MiniappJwtUtils.class);

    /**
     * 小程序专用签名密钥（Base64），与管理端 JWT 密钥不同。
     * 开发环境使用内置默认值，生产环境通过环境变量 JWT_MINIAPP_SECRET 注入。
     */
    @Value("${jwt.miniapp-secret:${JWT_MINIAPP_SECRET:TWluaUFwcFBsYXRmb3JtU2VjcmV0S2V5Rm9ySldUVG9rZW5HZW5lcmF0aW9uMjAyNl9NaW5pQXBw}}")
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
     * 小程序 JWT 密钥安全校验（profile 无关）。
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
                "TWluaUFwcFBsYXRmb3JtU2VjcmV0S2V5Rm9ySldUVG9rZW5HZW5lcmF0aW9uMjAyNl9NaW5pQXBw";

        // ── 校验1：密钥为空或长度不足（HMAC-SHA256 要求 ≥256 bits = 44 Base64 字符） ──
        if (secretBase64 == null || secretBase64.length() < 44) {
            throw new IllegalStateException(
                    "\n============================================================\n"
                    + "❌ 致命错误：JWT 小程序密钥未配置或长度不足（至少 44 字符 Base64）！\n"
                    + "   请设置环境变量 JWT_MINIAPP_SECRET。\n"
                    + "   生成命令：openssl rand -base64 32\n"
                    + "   开发环境可在 application.yml 中设置 jwt.miniapp-secret 固定值。\n"
                    + "   应用将拒绝启动。\n"
                    + "============================================================"
            );
        }

        // ── 校验2：密钥为已知硬编码默认值（源码中的 @Value 嵌套默认值） ──
        if (KNOWN_DEFAULT.equals(secretBase64)) {
            throw new IllegalStateException(
                    "\n============================================================\n"
                    + "❌ 致命错误：使用了默认 JWT 小程序密钥，存在严重安全风险！\n"
                    + "   请设置环境变量 JWT_MINIAPP_SECRET 覆盖默认值。\n"
                    + "   生成命令：openssl rand -base64 32\n"
                    + "   应用将拒绝启动。\n"
                    + "============================================================"
            );
        }
    }

    /** Token 有效期：30 天（毫秒） */
    private static final long EXPIRATION_MS = 30L * 24 * 60 * 60 * 1000L;

    // ────────────────── 生成 Token ──────────────────

    /**
     * 为小程序用户生成 JWT Token。
     *
     * @param userId     小程序用户主键ID
     * @param customerId 绑定的商户ID
     * @param loginType  登录方式: WECHAT / PHONE
     * @return 加密后的 JWT 字符串
     */
    public String generateToken(Long userId, Long customerId, String loginType, Integer tokenVersion) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("customerId", customerId)
                .claim("loginType", loginType)
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
     * 从 Token 中提取小程序用户ID。
     */
    public Long getUserIdFromToken(String token) {
        String subject = parseToken(token).getSubject();
        return Long.valueOf(subject);
    }

    /**
     * 从 Token 中提取商户ID。
     */
    public Long getCustomerIdFromToken(String token) {
        return parseToken(token).get("customerId", Long.class);
    }

    /**
     * 从 Token 中提取登录方式。
     */
    public String getLoginTypeFromToken(String token) {
        return parseToken(token).get("loginType", String.class);
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
            log.warn("小程序 JWT 已过期: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("小程序 JWT 格式错误: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("小程序 JWT 签名无效: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("小程序 JWT 参数非法: {}", e.getMessage());
        }
        return false;
    }
}
