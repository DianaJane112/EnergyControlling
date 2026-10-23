package com.competition.energycontrollerplatform.security;

import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.entity.User;
import com.competition.energycontrollerplatform.mapper.UserMapper;
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
 * JWT 认证过滤器。
 * <p>
 * 继承 {@link OncePerRequestFilter} 确保每个请求只过滤一次。
 * 从 Authorization 头中提取 Bearer Token，校验签名+过期后
 * 再校验 tokenVersion（与数据库中的版本比对），防止修改密码/登出后旧 Token 继续使用。
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final SecurityContextRepository securityContextRepository;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * CORS 允许的来源域名（注入自 {@code cors.allowed-origins}），
     * 用于 writeUnauthorized() 中对 401 响应做 CORS 白名单校验。
     */
    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    public JwtAuthenticationFilter(JwtUtils jwtUtils,
                                   SecurityContextRepository securityContextRepository,
                                   UserMapper userMapper,
                                   StringRedisTemplate stringRedisTemplate) {
        this.jwtUtils = jwtUtils;
        this.securityContextRepository = securityContextRepository;
        this.userMapper = userMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 小程序路径由 MiniappJwtAuthenticationFilter 处理，直接放行，避免密钥冲突导致 401
        if (request.getRequestURI().startsWith("/api/v1/miniapp")) {
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
            if (!jwtUtils.validateToken(token)) {
                writeUnauthorized(request, response, "Token 无效或已过期");
                return;
            }

            // 提取用户信息
            Long userId = jwtUtils.getUserIdFromToken(token);
            String username = jwtUtils.getUsernameFromToken(token);
            String role = jwtUtils.getRoleFromToken(token);

            // ── tokenVersion 校验（修改密码/登出后旧 Token 即时报废） ──
            Integer tokenVersion = jwtUtils.getTokenVersionFromToken(token);
            if (tokenVersion == null) {
                // 旧 Token 无 tokenVersion 声明 → 拒绝（迁移后强制所有用户重新登录）
                writeUnauthorized(request, response, "Token 格式已升级，请重新登录");
                return;
            }
            // 先从 Redis 缓存查 tokenVersion（5分钟 TTL），miss 时才查 DB
            String cacheKey = "token_version:user:" + userId;
            try {
                String cached = stringRedisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    if (Integer.parseInt(cached) != tokenVersion) {
                        writeUnauthorized(request, response, "Token 已失效（密码已修改或账号已登出），请重新登录");
                        return;
                    }
                } else {
                    User user = userMapper.selectById(userId);
                    if (user == null || !tokenVersion.equals(user.getTokenVersion())) {
                        writeUnauthorized(request, response, "Token 已失效（密码已修改或账号已登出），请重新登录");
                        return;
                    }
                    // 缓存到 Redis，5 分钟过期
                    stringRedisTemplate.opsForValue().set(cacheKey,
                            String.valueOf(user.getTokenVersion()), 5, TimeUnit.MINUTES);
                }
            } catch (Exception e) {
                // Redis 不可用时降级查 DB（不过滤 Redis 异常导致所有请求被拒绝）
                log.warn("tokenVersion 缓存查询失败，降级查 DB: {}", e.getMessage());
                User user = userMapper.selectById(userId);
                if (user == null || !tokenVersion.equals(user.getTokenVersion())) {
                    writeUnauthorized(request, response, "Token 已失效，请重新登录");
                    return;
                }
            }

            // 构建认证对象并注入 SecurityContext
            String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            new JwtUserPrincipal(userId, username, role),
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(roleWithPrefix))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            // 将认证上下文保存到 SecurityContextRepository，
            // 防止 SecurityContextHolderFilter 用空上下文将其覆盖
            SecurityContext context = SecurityContextHolder.getContext();
            securityContextRepository.saveContext(context, request, response);
            log.debug("JWT 认证成功: userId={}, username={}, role={}", userId, username, role);

        } catch (Exception e) {
            log.warn("JWT 解析异常: {}", e.getMessage());
            writeUnauthorized(request, response, "Token 解析失败");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 Bearer Token。
     * <p>
     * 兼容两种格式：
     * <ul>
     *   <li>标准格式：{@code Authorization: Bearer <token>}</li>
     *   <li>Knife4j APIKEY 格式：{@code Authorization: <token>}（直接粘贴原始 Token）</li>
     * </ul>
     * 防御性校验：Header 可能为 null、空字符串、或包含前导空白字符。
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
        // 没有 Bearer 前缀时，直接把整个 header 值当作 Token（兼容 Knife4j APIKEY 模式）
        return trimmed;
    }

    /**
     * 返回 401 未认证 JSON 响应，不继续走过滤器链。
     * <p>
     * 防御性检查：若响应已提交（被前置过滤器写入），则跳过写入避免异常。
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
