package com.competition.energycontrollerplatform.common;

import com.competition.energycontrollerplatform.common.annotation.Idempotent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 幂等性拦截器。
 * <p>
 * 拦截标注了 {@link Idempotent} 注解的 Controller 方法，
 * 使用 Redis {@code SET NX EX} 原子操作检查和处理幂等性 Key。
 * <p>
 * <b>架构顺序：</b>本拦截器通过 {@code WebMvcConfigurer.addInterceptors()} 注册，
 * 以 {@code order=100} 置于 HandlerInterceptor 链末端。Servlet Filter 层的
 * JWT 鉴权（{@code JwtAuthenticationFilter}）天然先于 HandlerInterceptor 执行，
 * 未认证请求会被 401 拦截，不会到达本拦截器消耗 Redis 资源。
 * <p>
 * <b>Key 格式：</b>{@code idempotent:<X-Idempotency-Key值>}<br>
 * <b>响应缓存：</b>{@code idempotent:resp:<X-Idempotency-Key值>}
 * <p>
 * <b>容灾：</b>Redis 不可用时打印 WARN 日志并放行请求（Fail-open），优先保证业务可用。
 */
@Component
@RequiredArgsConstructor
public class IdempotentInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(IdempotentInterceptor.class);
    private static final String HEADER_IDEMPOTENCY_KEY = "X-Idempotency-Key";
    private static final String REDIS_PREFIX = "idempotent:";
    private static final String RESP_PREFIX = "idempotent:resp:";
    private static final String REQUEST_ATTR_KEY = "idempotentKey";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Idempotent annotation = handlerMethod.getMethodAnnotation(Idempotent.class);
        if (annotation == null) {
            return true;
        }

        String idempotencyKey = request.getHeader(HEADER_IDEMPOTENCY_KEY);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            writeError(response, 400, "缺少 X-Idempotency-Key 请求头，该接口要求幂等性 Key");
            return false;
        }

        // 防止 Key 注入：限制长度
        if (idempotencyKey.length() > 128) {
            writeError(response, 400, "X-Idempotency-Key 长度超过限制（最大128字符）");
            return false;
        }

        String redisKey = REDIS_PREFIX + idempotencyKey;
        long ttl = annotation.ttlSeconds();

        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(redisKey, "1", ttl, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(acquired)) {
                // 首次请求 → 放行，记录 key 供 afterCompletion 缓存响应
                request.setAttribute(REQUEST_ATTR_KEY, idempotencyKey);
                request.setAttribute("idempotentTtl", ttl);
                return true;
            }

            // 重复请求 → 返回缓存的响应
            String cachedResponse = stringRedisTemplate.opsForValue()
                    .get(RESP_PREFIX + idempotencyKey);
            if (cachedResponse != null) {
                response.setStatus(annotation.conflictStatus());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(cachedResponse);
            } else {
                writeError(response, annotation.conflictStatus(),
                        "重复请求，请使用上一次请求的结果");
            }
            log.warn("幂等性拦截: key={}, 重复请求已拒绝", idempotencyKey);
            return false;
        } catch (Exception e) {
            // Redis 不可用 → Fail-open，放行请求优先保证业务
            log.warn("[幂等性] Redis 不可用，降级放行。key={}, error={}",
                    idempotencyKey, e.getMessage());
            return true;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        String idempotencyKey = (String) request.getAttribute(REQUEST_ATTR_KEY);
        if (idempotencyKey == null) {
            return;
        }
        Long ttl = (Long) request.getAttribute("idempotentTtl");
        if (ttl == null) ttl = 60L;

        // 只缓存成功响应（2xx），异常响应不缓存
        if (ex != null || response.getStatus() >= 300) {
            return;
        }

        try {
            // 从 ContentCachingResponseWrapper 读取响应体
            if (response instanceof ContentCachingResponseWrapper wrapper) {
                byte[] content = wrapper.getContentAsByteArray();
                if (content.length > 0 && content.length <= 65536) { // 最多缓存 64KB
                    String body = new String(content, StandardCharsets.UTF_8);
                    stringRedisTemplate.opsForValue().set(
                            RESP_PREFIX + idempotencyKey, body, ttl, TimeUnit.SECONDS);
                }
            }
        } catch (Exception e) {
            log.warn("[幂等性] 缓存响应失败 key={}, error={}", idempotencyKey, e.getMessage());
        }
    }

    private void writeError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> result = Result.error(status, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
