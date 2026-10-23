package com.competition.energycontrollerplatform.miniapp.security;

import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.miniapp.entity.MiniAppUser;
import com.competition.energycontrollerplatform.miniapp.mapper.MiniAppUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 小程序 JWT 认证过滤器。
 * <p>
 * 仅处理 /api/v1/miniapp/** 路径的请求。
 * 非此路径直接放行给下一个过滤器（管理员 JWT 过滤器）。
 * 从 Authorization 头中提取 Bearer Token，校验签名+过期后
 * 再校验 tokenVersion，防止登出后旧 Token 继续使用。
 */
@Slf4j
public class MiniappJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String MINIAPP_API_PREFIX = "/api/v1/miniapp";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final MiniappJwtUtils miniappJwtUtils;
    private final SecurityContextRepository securityContextRepository;
    private final MiniAppUserMapper miniAppUserMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * CORS 允许的来源域名（注入自 {@code cors.allowed-origins}），
     * 用于 writeUnauthorized() 中对 401 响应做 CORS 白名单校验。
     */
    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    public MiniappJwtAuthenticationFilter(MiniappJwtUtils miniappJwtUtils,
                                          SecurityContextRepository securityContextRepository,
                                          MiniAppUserMapper miniAppUserMapper,
                                          StringRedisTemplate stringRedisTemplate) {
        this.miniappJwtUtils = miniappJwtUtils;
        this.securityContextRepository = securityContextRepository;
        this.miniAppUserMapper = miniAppUserMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 仅处理小程序 API 路径，其他路径直接放行
        String requestUri = request.getRequestURI();
        if (!requestUri.startsWith(MINIAPP_API_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // OPTIONS 预检请求直接放行，由 Spring Security CorsFilter 处理
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);

        if (token == null) {
            // 无 Token → 继续走过滤器链（由 SecurityConfig 决定是否放行）
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 校验 Token 合法性（签名 + 过期）
            if (!miniappJwtUtils.validateToken(token)) {
                writeUnauthorized(request, response, "小程序 Token 无效或已过期");
                return;
            }

            // 提取用户信息
            Long userId = miniappJwtUtils.getUserIdFromToken(token);
            Long customerId = miniappJwtUtils.getCustomerIdFromToken(token);
            String loginType = miniappJwtUtils.getLoginTypeFromToken(token);

            // ── tokenVersion 校验 ──
            Integer tokenVersion = miniappJwtUtils.getTokenVersionFromToken(token);
            if (tokenVersion == null) {
                writeUnauthorized(request, response, "Token 格式已升级，请重新登录");
                return;
            }
            String cacheKey = "token_version:miniapp_user:" + userId;
            try {
                String cached = stringRedisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    if (Integer.parseInt(cached) != tokenVersion) {
                        writeUnauthorized(request, response, "Token 已失效，请重新登录");
                        return;
                    }
                } else {
                    MiniAppUser user = miniAppUserMapper.selectById(userId);
                    if (user == null || !tokenVersion.equals(user.getTokenVersion())) {
                        writeUnauthorized(request, response, "Token 已失效，请重新登录");
                        return;
                    }
                    stringRedisTemplate.opsForValue().set(cacheKey,
                            String.valueOf(user.getTokenVersion()), 5, TimeUnit.MINUTES);
                }
            } catch (Exception e) {
                log.warn("小程序 tokenVersion 缓存查询失败，降级查 DB: {}", e.getMessage());
                MiniAppUser user = miniAppUserMapper.selectById(userId);
                if (user == null || !tokenVersion.equals(user.getTokenVersion())) {
                    writeUnauthorized(request, response, "Token 已失效，请重新登录");
                    return;
                }
            }

            // 构建认证对象并注入 SecurityContext
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            new MiniAppUserPrincipal(userId, customerId, loginType),
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_MINIPROGRAM_USER"))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            // 将认证上下文保存到 SecurityContextRepository，
            // 防止 SecurityContextHolderFilter 用空上下文将其覆盖
            SecurityContext context = SecurityContextHolder.getContext();
            securityContextRepository.saveContext(context, request, response);
            log.debug("小程序 JWT 认证成功: userId={}, customerId={}, loginType={}", userId, customerId, loginType);

        } catch (Exception e) {
            log.warn("小程序 JWT 解析异常: {}", e.getMessage());
            writeUnauthorized(request, response, "小程序 Token 解析失败");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Bearer Token。
     * <p>
     * 兼容标准 Bearer 格式和 Knife4j APIKEY 模式（无 Bearer 前缀）。
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header == null || header.isBlank()) {
            return null;
        }
        String trimmed = header.trim();
        if (trimmed.startsWith(BEARER_PREFIX)) {
            String token = trimmed.substring(BEARER_PREFIX.length());
            return token.isBlank() ? null : token;
        }
        // 兼容 Knife4j APIKEY 模式（直接粘贴原始 Token）
        return trimmed;
    }

    /**
     * 返回 401 未认证 JSON 响应，不继续走过滤器链。
     * <p>
     * 仅当请求 Origin 在 {@code cors.allowed-origins} 白名单内（或白名单为 *）时
     * 才回显 CORS 响应头，防止任意来源的端点探测。
     */
    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        // 添加 CORS 响应头（此过滤器在 Spring Security CorsFilter 之前执行，
        // 若在此处截断请求，必须手动添加 CORS 头，否则浏览器会拦截响应）
        // 仅当 Origin 在白名单内时才回显，防止任意来源的端点探测
        String origin = request.getHeader("Origin");
        boolean originAllowed = origin != null && !origin.isBlank() && isOriginAllowed(origin);
        if (originAllowed) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        } else if ("*".equals(allowedOrigins)) {
            response.setHeader("Access-Control-Allow-Origin", "*");
        }
        if (originAllowed || "*".equals(allowedOrigins)) {
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With, Accept, Origin, Access-Control-Request-Method, Access-Control-Request-Headers");
            response.setHeader("Access-Control-Expose-Headers", "Authorization, Content-Type, Content-Disposition");
            response.setHeader("Access-Control-Max-Age", "3600");
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Result<Void> result = Result.error(Result.CODE_UNAUTHORIZED, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    /**
     * 检查请求 Origin 是否在配置的 CORS 白名单中。
     * <p>未配置（空字符串）→ 拒绝所有；显式配置 * → 允许所有；其他 → 精确匹配。</p>
     */
    private boolean isOriginAllowed(String origin) {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return false;
        }
        if ("*".equals(allowedOrigins.trim())) {
            return true;
        }
        return Arrays.stream(allowedOrigins.split(","))
                     .map(String::trim)
                     .anyMatch(pattern -> pattern.equals(origin));
    }
}
