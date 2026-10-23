package com.competition.energycontrollerplatform.common;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 空值缓存辅助工具。
 * <p>
 * 防止缓存穿透：当 DB 查询返回 null 时，在 Redis 中缓存一个短 TTL 的 "NULL" 标记，
 * 后续相同查询直接返回"不存在"而不穿透到 DB。
 * <p>
 * Key 格式：{@code cache:null:<entityType>:<id>}，默认 TTL 30 秒。
 */
@Component
@RequiredArgsConstructor
public class CacheNullValueHelper {

    private static final String NULL_PREFIX = "cache:null:";
    private static final long DEFAULT_TTL_SEC = 30;

    private final StringRedisTemplate stringRedisTemplate;

    /** 缓存一个空值标记 */
    public void cacheNull(String entityType, Long id, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(
                NULL_PREFIX + entityType + ":" + id, "NULL", ttlSeconds, TimeUnit.SECONDS);
    }

    /** 缓存空值（使用默认 30 秒 TTL） */
    public void cacheNull(String entityType, Long id) {
        cacheNull(entityType, id, DEFAULT_TTL_SEC);
    }

    /** 检查是否已缓存了空值 */
    public boolean isNullCached(String entityType, Long id) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(NULL_PREFIX + entityType + ":" + id));
    }

    /** 清除空值缓存（实体被创建时应清除） */
    public void evictNull(String entityType, Long id) {
        stringRedisTemplate.delete(NULL_PREFIX + entityType + ":" + id);
    }
}
