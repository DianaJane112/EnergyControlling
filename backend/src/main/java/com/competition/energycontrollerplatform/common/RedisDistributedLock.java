package com.competition.energycontrollerplatform.common;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的轻量级分布式锁。
 * <p>
 * <b>实现原理：</b>
 * <ul>
 *   <li>加锁 = {@code SET lockKey token NX EX timeoutSec}（SETNX + TTL 原子操作）</li>
 *   <li>解锁 = Lua 脚本校验 token 后删除（防止误删其他实例的锁）</li>
 *   <li>锁超时自动过期，防止进程崩溃导致死锁</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * String token = lock.tryLock("billing:daily", 300);
 * if (token != null) {
 *     try {
 *         // 执行业务逻辑
 *     } finally {
 *         lock.unlock("billing:daily", token);
 *     }
 * }
 * }</pre>
 * <p>
 * <b>与 ShedLock 的区别：</b>
 * ShedLock 需要额外表 / 额外依赖，本工具直接复用项目已有的 Redis，
 * 更适合轻量场景。如需更强大的锁（可重入、公平锁等），建议引入 Redisson。
 */
@Component
@RequiredArgsConstructor
public class RedisDistributedLock {

    private static final Logger log = LoggerFactory.getLogger(RedisDistributedLock.class);

    private final StringRedisTemplate stringRedisTemplate;

    /** Lua 脚本：仅当 value 匹配时才删除 key（防止误删其他实例持有的锁） */
    private static final String UNLOCK_SCRIPT = """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            else
                return 0
            end""";

    /**
     * 尝试获取分布式锁。
     *
     * @param lockKey    锁的 Redis key（建议带业务前缀，如 "lock:billing:daily"）
     * @param timeoutSec 锁持有超时时间（秒），超过后自动释放，防止死锁
     * @return 锁令牌（解锁时需传入），获取失败返回 {@code null}
     */
    public String tryLock(String lockKey, long timeoutSec) {
        String token = UUID.randomUUID().toString();
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, token, timeoutSec, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(success)) {
            log.debug("分布式锁获取成功 key={}, timeout={}s", lockKey, timeoutSec);
            return token;
        }
        log.debug("分布式锁获取失败（已被其他实例持有）key={}", lockKey);
        return null;
    }

    /**
     * 释放分布式锁（仅当 token 匹配时才释放）。
     *
     * @param lockKey 锁的 Redis key
     * @param token   加锁时返回的令牌
     */
    public void unlock(String lockKey, String token) {
        if (token == null) {
            return;
        }
        try {
            Long result = stringRedisTemplate.execute(
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>(
                            UNLOCK_SCRIPT, Long.class),
                    java.util.Collections.singletonList(lockKey),
                    token);
            if (result != null && result == 1) {
                log.debug("分布式锁释放成功 key={}", lockKey);
            } else {
                log.debug("分布式锁释放跳过（锁已过期或被其他实例持有）key={}", lockKey);
            }
        } catch (Exception e) {
            log.error("分布式锁释放异常 key={}", lockKey, e);
        }
    }
}
