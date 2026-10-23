package com.competition.energycontrollerplatform.testbase;

import com.competition.energycontrollerplatform.common.BloomFilterHelper;
import com.competition.energycontrollerplatform.common.CacheNullValueHelper;
import com.competition.energycontrollerplatform.common.RedisDistributedLock;
import com.competition.energycontrollerplatform.common.RateLimiter;
import com.competition.energycontrollerplatform.integration.hook.ValveControlHook;
import com.competition.energycontrollerplatform.netty.NettyServer;
import org.mockito.Mockito;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 测试专用配置 —— 提供 mock / no-op Bean，避免依赖 Redis / Netty / 外部服务。
 */
@TestConfiguration
@Profile("test")
public class TestMockBeans {

    @Bean
    @Primary
    public NettyServer nettyServer() {
        return Mockito.mock(NettyServer.class);
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        return Mockito.mock(StringRedisTemplate.class);
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> redisTemplate = Mockito.mock(RedisTemplate.class);
        return redisTemplate;
    }

    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new NoOpCacheManager();
    }

    @Bean
    @Primary
    public BloomFilterHelper bloomFilterHelper() {
        return Mockito.mock(BloomFilterHelper.class);
    }

    @Bean
    @Primary
    public CacheNullValueHelper cacheNullValueHelper() {
        return Mockito.mock(CacheNullValueHelper.class);
    }

    @Bean
    @Primary
    public RedisDistributedLock redisDistributedLock() {
        return Mockito.mock(RedisDistributedLock.class);
    }

    @Bean
    @Primary
    public RateLimiter rateLimiter() {
        return Mockito.mock(RateLimiter.class);
    }

    @Bean
    @Primary
    public ValveControlHook testValveControlHook() {
        return Mockito.mock(ValveControlHook.class);
    }
}
