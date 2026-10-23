package com.competition.energycontrollerplatform.common.annotation;

import java.lang.annotation.*;

/**
 * 幂等性保护注解。
 * <p>
 * 标注在 Controller 方法上，由 {@link com.competition.energycontrollerplatform.common.IdempotentInterceptor}
 * 拦截处理。客户端需在请求头中携带 {@code X-Idempotency-Key}。
 * <p>
 * 使用 Redis {@code SET NX EX} 原子注册幂等性 Key，防止重复提交。
 * 当检测到重复请求时，返回缓存的响应或 409 Conflict。
 * <p>
 * <b>安全顺序：</b>JWT 鉴权 Filter 先于本拦截器执行，未认证请求会在到达前被 401 拦截，
 * 不会消耗 Redis 资源。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /** 幂等性 Key 的 TTL（秒），默认 60 秒 */
    long ttlSeconds() default 60;

    /** 重复请求时返回的 HTTP 状态码，默认 409 Conflict */
    int conflictStatus() default 409;
}
