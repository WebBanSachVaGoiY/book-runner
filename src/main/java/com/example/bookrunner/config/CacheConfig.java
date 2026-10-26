package com.example.bookrunner.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Cấu hình cache cho tầng gợi ý.
 *
 * <p>Ưu tiên Redis (theo RECSYS_DOCUMENTATION §9). Nếu không kết nối được Redis,
 * tự động fallback sang {@link ConcurrentMapCacheManager} (RAM) để service KHÔNG
 * bao giờ crash. Ngoài ra {@link CacheErrorHandler} nuốt mọi lỗi cache runtime
 * (log cảnh báo) để gợi ý vẫn hoạt động khi Redis chết giữa chừng.
 */
@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig implements CachingConfigurer {

    private final RecSysProperties recSysProperties;
    private final ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider;

    @Override
    @Bean
    public CacheManager cacheManager() {
        RedisConnectionFactory connectionFactory = redisConnectionFactoryProvider.getIfAvailable();
        if (connectionFactory != null && isRedisAvailable(connectionFactory)) {
            log.info("✅ RecSys cache dùng Redis (for-you TTL = {}h).", recSysProperties.getCacheTtlHours());
            return buildRedisCacheManager(connectionFactory);
        }
        log.warn("⚠️ Không kết nối được Redis → RecSys cache fallback sang ConcurrentMapCacheManager (RAM).");
        return new ConcurrentMapCacheManager();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        // Không bao giờ để lỗi cache làm hỏng request gợi ý.
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache GET lỗi [{}] key={}: {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache PUT lỗi [{}] key={}: {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache EVICT lỗi [{}] key={}: {}", cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache CLEAR lỗi [{}]: {}", cache.getName(), exception.getMessage());
            }
        };
    }

    private boolean isRedisAvailable(RedisConnectionFactory connectionFactory) {
        RedisConnection connection = null;
        try {
            connection = connectionFactory.getConnection();
            connection.ping();
            return true;
        } catch (Exception e) {
            log.debug("Redis ping thất bại: {}", e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignored) {
                    // bỏ qua
                }
            }
        }
    }

    private RedisCacheManager buildRedisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisSerializer<Object> valueSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableUnsafeDefaultTyping()
                .build();
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .disableCachingNullValues();

        Duration forYouTtl = Duration.ofHours(Math.max(1, recSysProperties.getCacheTtlHours()));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put(CacheNames.FOR_YOU, base.entryTtl(forYouTtl));
        perCache.put(CacheNames.RELATED, base.entryTtl(Duration.ofHours(12)));
        perCache.put(CacheNames.FBT, base.entryTtl(Duration.ofHours(12)));
        perCache.put(CacheNames.TRENDING, base.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.entryTtl(forYouTtl))
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}
