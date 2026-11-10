package com.example.bookrunner.config;

import com.example.bookrunner.dto.response.BookResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RedisCacheIntegrationTest {

    @Test
    @DisplayName("Kiểm thử Serialization trực tiếp của GenericJacksonJsonRedisSerializer")
    void testSerializationDirectly() {
        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder().build();

        BookResponseDTO book1 = new BookResponseDTO();
        book1.setId(101L);
        book1.setTitle("Học Máy & Trí Tuệ Nhân Tạo");
        book1.setAuthor("Nguyễn Văn A");
        book1.setPrice(new BigDecimal("150000"));
        book1.setAverageRating(4.8);
        book1.setSoldCount(50);
        book1.setCreatedAt(LocalDateTime.now());

        List<BookResponseDTO> list = List.of(book1);

        byte[] bytes = serializer.serialize(list);
        assertNotNull(bytes);

        Object deserialized = serializer.deserialize(bytes);
        assertNotNull(deserialized);
        assertTrue(deserialized instanceof List);
        @SuppressWarnings("unchecked")
        List<?> deserializedList = (List<?>) deserialized;
        assertEquals(1, deserializedList.size());
    }

    @Test
    @DisplayName("Kiểm thử hoạt động thực tế với Redis CacheManager (Put, Get, Evict, Clear)")
    void testRedisLiveConnectionAndCacheOperations() throws Exception {
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration("localhost", 6379)
        );
        boolean isConnected = false;
        try {
            connectionFactory.afterPropertiesSet();
            String pingResult = connectionFactory.getConnection().ping();
            isConnected = "PONG".equalsIgnoreCase(pingResult);
        } catch (Exception e) {
            isConnected = false;
        }

        assumeTrue(isConnected, "Bỏ qua test vì Redis server không chạy tại localhost:6379");

        try {
            CacheConfig cacheConfig = new CacheConfig(new RecSysProperties(), null);
            RedisCacheManager cacheManager = cacheConfig.buildRedisCacheManager(connectionFactory);
            assertNotNull(cacheManager);

            Cache trendingCache = cacheManager.getCache(CacheNames.TRENDING);
            assertNotNull(trendingCache, "Cache TRENDING phải tồn tại trong RedisCacheManager");

            String cacheKey = "live:trending:test";

            BookResponseDTO book = new BookResponseDTO();
            book.setId(999L);
            book.setTitle("Sách Thử Nghiệm Redis");
            book.setAuthor("Tác giả Redis");
            book.setPrice(new BigDecimal("99000"));
            List<BookResponseDTO> books = List.of(book);

            // 1. PUT
            trendingCache.put(cacheKey, books);

            // 2. GET
            Cache.ValueWrapper wrapper = trendingCache.get(cacheKey);
            assertNotNull(wrapper, "Dữ liệu phải có trong Redis cache sau khi PUT");
            Object cachedValue = wrapper.get();
            assertNotNull(cachedValue);
            assertTrue(cachedValue instanceof List, "Dữ liệu lấy từ cache phải là List");
            @SuppressWarnings("unchecked")
            List<?> cachedList = (List<?>) cachedValue;
            assertEquals(1, cachedList.size());

            // 3. EVICT
            trendingCache.evict(cacheKey);
            Cache.ValueWrapper afterEvict = trendingCache.get(cacheKey);
            assertNull(afterEvict, "Dữ liệu phải bị xóa sau khi EVICT");

            // 4. CLEAR (Thử nghiệm invalidate và đợi clear async hoàn tất)
            trendingCache.put("live:trending:key1", books);
            trendingCache.put("live:trending:key2", books);
            assertNotNull(trendingCache.get("live:trending:key1"));
            assertNotNull(trendingCache.get("live:trending:key2"));

            if (trendingCache instanceof RedisCache redisCache) {
                boolean invalidated = redisCache.invalidate();
                System.out.println("redisCache.invalidate() returned: " + invalidated);
            } else {
                trendingCache.clear();
            }

            // Đợi tối đa 1.5s cho bất kỳ write bất đồng bộ nào kết thúc
            int retries = 0;
            while (trendingCache.get("live:trending:key1") != null && retries < 15) {
                Thread.sleep(100);
                retries++;
            }

            assertNull(trendingCache.get("live:trending:key1"), "Key1 phải null sau khi CLEAR");
            assertNull(trendingCache.get("live:trending:key2"), "Key2 phải null sau khi CLEAR");

        } finally {
            connectionFactory.destroy();
        }
    }
}
