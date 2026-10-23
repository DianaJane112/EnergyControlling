package com.competition.energycontrollerplatform.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 配置。
 * <p>
 * 提供三种能力：
 * <ul>
 *   <li>{@link StringRedisTemplate} — 纯字符串操作（验证码、计数器、分布式锁）</li>
 *   <li>{@link RedisTemplate} — JSON 序列化对象操作（缓存 DTO/VO）</li>
 *   <li>{@link CacheManager} — Spring Cache 声明式缓存（Dashboard / 用户 / 商户 / 费率）</li>
 * </ul>
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {

    /**
     * 通用 RedisTemplate（Key=String, Value=Object → JSON）。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(mapper);

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    /**
     * Redis 缓存管理器 — 按 cacheName 分配不同 TTL。
     * <table>
     *   <tr><th>cacheName</th><th>TTL</th><th>说明</th></tr>
     *   <tr><td>dashboard</td><td>3 分钟</td><td>Dashboard 聚合数据，需要一定实时性</td></tr>
     *   <tr><td>meter_statistics</td><td>3 分钟</td><td>表计统计概览</td></tr>
     *   <tr><td>user</td><td>10 分钟</td><td>用户信息，极少变更</td></tr>
     *   <tr><td>customer</td><td>10 分钟</td><td>商户信息，偶有变更</td></tr>
     *   <tr><td>price_strategy</td><td>30 分钟</td><td>费率策略，几乎不变</td></tr>
     *   <tr><td>default</td><td>5 分钟</td><td>兜底 TTL</td></tr>
     * </table>
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // JSON 序列化器（注册 JavaTimeModule 支持 LocalDateTime 等类型）
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        // 各 cacheName 独立 TTL
        Map<String, RedisCacheConfiguration> ttlMap = new HashMap<>();
        ttlMap.put("dashboard", defaultConfig.entryTtl(Duration.ofMinutes(3)));
        ttlMap.put("meter_statistics", defaultConfig.entryTtl(Duration.ofMinutes(3)));
        ttlMap.put("user", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        ttlMap.put("customer", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        ttlMap.put("price_strategy", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(ttlMap)
                .build();
    }
}
