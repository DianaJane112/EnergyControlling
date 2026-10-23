package com.competition.energycontrollerplatform.common;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的简易频率限制器。
 * <p>
 * 使用场景：
 * <ul>
 *   <li>登录接口防爆破</li>
 *   <li>短信验证码发送频率限制</li>
 *   <li>验证码尝试次数限制</li>
 * </ul>
 * <p>
 * 实现：Redis 计数器 + TTL 过期，无需依赖第三方库。
 * <p>
 * <b>容灾策略（Fail-open）：</b>
 * 当 Redis 不可用（宕机/网络异常）时，{@link #tryAcquire(String, int, long)} 会
 * 打印 ERROR 日志并返回 {@code true}（放行请求），优先保证核心业务可用性。
 * 其他方法（getCount / reset / setCode / consumeCode）在 Redis 异常时返回安全默认值
 * 并打印 WARN 日志，避免业务中断。
 * <p>
 * <b>原子性保证：</b>
 * {@code tryAcquire} 和 {@code consumeCode} 使用 Lua 脚本在 Redis 服务端原子执行，
 * 避免"INCR 成功但 EXPIRE 失败"导致 key 永不过期的孤儿-key 问题，
 * 同时兼容 Redis 2.6+ 全版本（无需依赖 Redis 6.2 的 GETDEL 命令）。
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Lua 脚本：原子执行 INCR + 条件 EXPIRE。
     * <p>
     * 消除 INCR 与 EXPIRE 之间的 TOCTOU 竞态窗口：
     * 若 Java 端先 INCR 成功再 EXPIRE 失败（连接池耗尽/网络闪断），key 将
     * 永不过期，导致对应 IP/用户名被永久限流。将两个操作合并为单次 EVAL
     * 原子执行后，要么两个都成功，要么两个都不执行。
     * <p>
     * 兼容 Redis 2.6+（Lua 脚本支持的最低版本）。
     */
    private static final String TRY_ACQUIRE_SCRIPT = """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """;

    /**
     * Lua 脚本：原子执行 GET + DEL（替代 Redis 6.2 的 GETDEL 命令）。
     * <p>
     * 使用 {@code redis.call('GET', KEYS[1])} + {@code redis.call('DEL', KEYS[1])}
     * 在服务端原子完成"读取并删除"，无需 Redis 6.2+。
     */
    private static final String CONSUME_CODE_SCRIPT = """
            local val = redis.call('GET', KEYS[1])
            if val then
                redis.call('DEL', KEYS[1])
            end
            return val
            """;

    /** tryAcquire Lua 脚本封装（Long 返回值，Spring 自动将 Redis integer 转为 Long） */
    private final DefaultRedisScript<Long> tryAcquireScript =
            new DefaultRedisScript<>(TRY_ACQUIRE_SCRIPT, Long.class);

    /** consumeCode Lua 脚本封装（String 返回值） */
    private final DefaultRedisScript<String> consumeCodeScript =
            new DefaultRedisScript<>(CONSUME_CODE_SCRIPT, String.class);

    /**
     * 尝试递增计数器。
     * <p>
     * <b>容灾：</b>Redis 不可用时返回 {@code true}（故障开放），
     * 优先保证登录/重置密码等核心业务可用。
     *
     * @param key        Redis key（如 "rate:login:ip:192.168.1.1"）
     * @param maxCount   最大允许次数
     * @param windowSec  时间窗口（秒），超时后计数器自动归零
     * @return true-允许通过，false-超过限制
     */
    public boolean tryAcquire(String key, int maxCount, long windowSec) {
        try {
            Long count = stringRedisTemplate.execute(
                    tryAcquireScript,
                    Collections.singletonList(key),
                    String.valueOf(windowSec));
            if (count == null) {
                log.warn("[RateLimiter] Redis EVAL 返回 null，降级放行 key={}", key);
                return true;
            }
            return count <= maxCount;
        } catch (Exception e) {
            log.error("[RateLimiter] Redis 异常，已触发故障开放（Fail-open），放行请求。"
                    + "key={}, maxCount={}, windowSec={}, error={}",
                    key, maxCount, windowSec, e.getMessage());
            return true;
        }
    }

    /**
     * 获取当前计数（不递增）。
     * <p>
     * <b>容灾：</b>Redis 不可用时返回 {@code 0L}。
     */
    public Long getCount(String key) {
        try {
            String val = stringRedisTemplate.opsForValue().get(key);
            if (val == null) return 0L;
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException e) {
                log.warn("[RateLimiter] 计数器值格式异常 key={}, val={}", key, val);
                return 0L;
            }
        } catch (Exception e) {
            log.warn("[RateLimiter] Redis 不可用，getCount 降级返回 0。key={}, error={}",
                    key, e.getMessage());
            return 0L;
        }
    }

    /**
     * 重置计数器。
     * <p>
     * <b>容灾：</b>Redis 不可用时仅打印 WARN 日志，不抛出异常。
     */
    public void reset(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("[RateLimiter] Redis 不可用，reset 操作跳过。key={}, error={}",
                    key, e.getMessage());
        }
    }

    /**
     * 设置验证码（带过期时间）。
     * <p>
     * <b>容灾：</b>Redis 不可用时打印 ERROR 日志（验证码功能降级），不抛出异常。
     * 调用方需自行处理后续逻辑（如提示用户稍后重试）。
     */
    public void setCode(String key, String code, long timeoutSec) {
        try {
            stringRedisTemplate.opsForValue().set(key, code, timeoutSec, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("[RateLimiter] Redis 不可用，验证码存储失败！key={}, error={}",
                    key, e.getMessage());
        }
    }

    /**
     * 获取并删除验证码（一次性使用，防止重放）。
     * <p>
     * 使用 Lua 脚本在 Redis 服务端原子执行 GET + DEL，兼容 Redis 2.6+。
     * <p>
     * <b>容灾：</b>Redis 不可用时返回 {@code null}（等同于验证码不存在/已过期），
     * 调用方会提示用户重新获取验证码。
     *
     * @return 验证码，不存在/已过期/Redis异常返回 null
     */
    public String consumeCode(String key) {
        try {
            return stringRedisTemplate.execute(
                    consumeCodeScript,
                    Collections.singletonList(key));
        } catch (Exception e) {
            log.warn("[RateLimiter] Redis 不可用，consumeCode 降级返回 null。key={}, error={}",
                    key, e.getMessage());
            return null;
        }
    }
}
