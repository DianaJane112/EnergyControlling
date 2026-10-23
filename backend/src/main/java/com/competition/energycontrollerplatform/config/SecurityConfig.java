package com.competition.energycontrollerplatform.config;

import com.competition.energycontrollerplatform.common.Result;
import com.competition.energycontrollerplatform.mapper.UserMapper;
import com.competition.energycontrollerplatform.miniapp.mapper.MiniAppUserMapper;
import com.competition.energycontrollerplatform.miniapp.security.MiniappJwtAuthenticationFilter;
import com.competition.energycontrollerplatform.miniapp.security.MiniappJwtUtils;
import com.competition.energycontrollerplatform.security.JwtAuthenticationFilter;
import com.competition.energycontrollerplatform.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Spring Security 核心配置。
 * <p>
 * 策略：完全无状态 JWT 认证 + BCrypt 密码加密。
 * 除登录和文档接口外，所有 /api/** 请求均需携带有效 Token。
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * CORS 允许的来源域名，多个以逗号分隔。
     * <p>默认空字符串 = 拒绝所有跨域请求（安全默认值）。
     * 开发环境在 {@code application.yml} 中显式配置为 {@code *}（允许任意来源）。
     * 生产环境通过 {@code CORS_ALLOWED_ORIGINS} 环境变量限制具体域名。</p>
     */
    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    /** Knife4j / Swagger 文档放行路径 */
    private static final String[] SWAGGER_WHITELIST = {
            "/doc.html",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
    };

    /** 无需认证的接口 */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/login",
            "/api/v1/auth/send-reset-code",
            "/api/v1/auth/reset-password",
            "/api/v1/miniapp/auth/wechat-login",
            "/api/v1/miniapp/auth/phone-login",
            "/api/v1/miniapp/auth/bind-customer",
            // 厂商 webhook（M2M 推送，无用户 Token）
            "/api/v1/integration/haopu/webhook",
    };

    // ────────────────── SecurityContextRepository ──────────────────

    /**
     * 将 SecurityContextRepository 显式暴露为 Bean，
     * 供 JWT 过滤器注入使用。
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new RequestAttributeSecurityContextRepository();
    }

    // ────────────────── Filter Bean 定义 ──────────────────

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtils jwtUtils,
                                                           SecurityContextRepository securityContextRepository,
                                                           UserMapper userMapper,
                                                           StringRedisTemplate stringRedisTemplate) {
        return new JwtAuthenticationFilter(jwtUtils, securityContextRepository, userMapper, stringRedisTemplate);
    }

    @Bean
    public MiniappJwtAuthenticationFilter miniappJwtAuthenticationFilter(
            MiniappJwtUtils miniappJwtUtils,
            SecurityContextRepository securityContextRepository,
            MiniAppUserMapper miniAppUserMapper,
            StringRedisTemplate stringRedisTemplate) {
        return new MiniappJwtAuthenticationFilter(miniappJwtUtils, securityContextRepository,
                miniAppUserMapper, stringRedisTemplate);
    }

    /**
     * 注册小程序 JWT 过滤器（作为 Servlet Filter，在 Spring Security 链之前执行）。
     * <p>
     * Spring Security 7 的 addFilterBefore 要求自定义 Filter 必须有注册顺序，
     * 改用 FilterRegistrationBean 将其注册为 Servlet Filter，优先级设为最高，
     * 确保在 Spring Security 过滤器链之前执行 JWT 认证。
     */
    @Bean
    public FilterRegistrationBean<MiniappJwtAuthenticationFilter> miniappFilterRegistration(
            MiniappJwtAuthenticationFilter filter) {
        FilterRegistrationBean<MiniappJwtAuthenticationFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        reg.addUrlPatterns("/*");
        return reg;
    }

    /**
     * 注册管理员 JWT 过滤器（作为 Servlet Filter，在 Spring Security 链之前执行）。
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        reg.addUrlPatterns("/*");
        return reg;
    }

    // ────────────────── SecurityFilterChain ──────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. 跨域配置（允许前端联调）
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 2. 统一认证/授权异常处理（返回结构化 JSON，避免 500）
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(customAuthenticationEntryPoint())
                    .accessDeniedHandler(customAccessDeniedHandler()))

            // 3. 关闭 CSRF（REST API 不需要）
            .csrf(AbstractHttpConfigurer::disable)

            // 4. 无状态会话管理
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 5. 路由权限配置
            .authorizeHttpRequests(auth -> auth
                    // 放行公开接口
                    .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                    // 放行 Swagger/Knife4j 文档
                    .requestMatchers(SWAGGER_WHITELIST).permitAll()
                    // OPTIONS 预检请求放行
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    // 其余所有 /api/** 需要认证
                    .requestMatchers("/api/**").authenticated()
                    // 其他请求放行（如静态资源）
                    .anyRequest().permitAll()
            )

            // 6. 禁用默认表单登录和 HTTP Basic
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    // ────────────────── 其他 Bean 声明 ──────────────────

    /**
     * BCrypt 密码编码器。
     * 用于登录时的密码匹配和创建用户时的密码加密。
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager 暴露为 Bean。
     * 当前登录流程通过 UserService 手动校验，暂不直接注入；
     * 保留此 Bean 供后续扩展（如 OAuth2、多认证方式）。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS 跨域配置。
     * <p>
     * 联调阶段允许任意来源访问（localhost:5173 / ngrok 穿透域名等）；
     * 正式上线前应将 allowedOriginPatterns 限制为具体域名。
     * <p>
     * 注意：{@code allowCredentials=true} 时不允许同时使用 {@code *} 通配符作为
     * {@code AllowedHeaders} 的值（浏览器 CORS 规范限制），因此显式列出所有需要的请求头。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 未配置 → 拒绝所有跨域请求（安全默认值）；显式配置 * 或具体域名 → 允许
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            log.warn("CORS 允许域名未配置，所有跨域请求将被拒绝。"
                    + "开发环境请在 application.yml 中设置 cors.allowed-origins: \"*\"，"
                    + "生产环境请通过 CORS_ALLOWED_ORIGINS 环境变量配置。");
        } else {
            // trim 每个条目，防止 "a.com, b.com" 中的空格导致静默 CORS 失败
            configuration.setAllowedOriginPatterns(
                    Arrays.stream(allowedOrigins.split(","))
                          .map(String::trim)
                          .filter(s -> !s.isEmpty())
                          .toList());
        }
        // 显式列出所有 HTTP 方法，确保 OPTIONS 预检通过
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // 显式列出允许的请求头（allowCredentials=true 时不能用 *）
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With",
                "Accept", "Origin", "Access-Control-Request-Method",
                "Access-Control-Request-Headers"));
        // 允许浏览器在 JS 中读取这些响应头
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        // 预检请求缓存 1 小时，减少 OPTIONS 请求次数
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // ────────────────── 认证/授权异常处理器 ──────────────────

    /**
     * 自定义未认证处理器。
     * <p>
     * 当用户未携带 Token 或 Token 已过期访问受保护接口时，
     * 返回结构化的 401 JSON 响应，而非默认的空白 403 或重定向页面。
     */
    private AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            Result<Void> result = Result.error(Result.CODE_UNAUTHORIZED, "未登录或 Token 已过期，请重新登录");
            response.getWriter().write(new ObjectMapper().writeValueAsString(result));
        };
    }

    /**
     * 自定义无权限处理器。
     * <p>
     * 当用户已认证但角色权限不足时，
     * 返回结构化的 403 JSON 响应。
     */
    private AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            Result<Void> result = Result.error(Result.CODE_FORBIDDEN, "权限不足，无法访问该资源");
            response.getWriter().write(new ObjectMapper().writeValueAsString(result));
        };
    }
}
