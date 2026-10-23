package com.competition.energycontrollerplatform.common;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * 基于 Redis BitSet 的布隆过滤器（无需 RedisBloom 模块）。
 * <p>
 * 用于快速判断 meter ID 是否存在，防止缓存穿透攻击。
 * 多个哈希函数将 ID 映射到 BitSet 的不同位置：
 * <ul>
 *   <li>所有位均为 1 → "可能存在"（约 1% 误判率）</li>
 *   <li>任一位为 0 → "绝对不存在"（可安全拒绝查询）</li>
 * </ul>
 * <p>
 * 参数：预计 10,000 元素，误判率 ~1% → 需要约 95,850 bits ≈ 12KB，7 个哈希函数。
 */
@Component
@RequiredArgsConstructor
public class BloomFilterHelper {

    private static final Logger log = LoggerFactory.getLogger(BloomFilterHelper.class);
    private static final String REDIS_KEY = "bloom:meter_ids";
    private static final int NUM_HASHES = 7;
    private static final long BIT_SIZE = 100000L;

    private final StringRedisTemplate stringRedisTemplate;

    /** 添加一个 ID 到布隆过滤器 */
    public void add(String id) {
        long[] positions = hash(id);
        for (long pos : positions) {
            stringRedisTemplate.opsForValue().setBit(REDIS_KEY, pos, true);
        }
    }

    /** 检查 ID 是否可能存在。返回 false → 绝对不存在；返回 true → 可能存在（约 1% 假阳性） */
    public boolean mightContain(String id) {
        long[] positions = hash(id);
        for (long pos : positions) {
            Boolean bit = stringRedisTemplate.opsForValue().getBit(REDIS_KEY, pos);
            if (Boolean.FALSE.equals(bit)) {
                return false;
            }
        }
        return true;
    }

    /** 批量重建布隆过滤器（启动时 + 每小时定时执行） */
    public void rebuild(List<String> allIds) {
        stringRedisTemplate.delete(REDIS_KEY);
        for (String id : allIds) {
            add(id);
        }
        log.info("Bloom Filter 重建完成，已加载 {} 个 meter ID", allIds.size());
    }

    private long[] hash(String value) {
        long[] result = new long[NUM_HASHES];
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < NUM_HASHES; i++) {
                long h = 0;
                for (int j = 0; j < 8 && (i * 8 + j) < digest.length; j++) {
                    h = (h << 8) | (digest[i * 8 + j] & 0xFF);
                }
                result[i] = Math.abs(h) % BIT_SIZE;
            }
        } catch (Exception e) {
            log.error("BloomFilter hash error", e);
        }
        return result;
    }
}
