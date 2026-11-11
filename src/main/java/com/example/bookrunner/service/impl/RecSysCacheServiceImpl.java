package com.example.bookrunner.service.impl;

import com.example.bookrunner.config.CacheNames;
import com.example.bookrunner.service.RecSysCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecSysCacheServiceImpl implements RecSysCacheService {

    private final CacheManager cacheManager;

    private static final List<String> ALL_RECSYS_CACHES = List.of(
            CacheNames.FOR_YOU,
            CacheNames.RELATED,
            CacheNames.FBT,
            CacheNames.TRENDING
    );

    @Override
    public void evictTrending() {
        clearCache(CacheNames.TRENDING);
    }

    @Override
    public void evictRelatedAndFbt(Long bookId) {
        clearCache(CacheNames.RELATED);
        clearCache(CacheNames.FBT);
    }

    @Override
    public void evictAllOnBookChange(Long bookId) {
        clearCache(CacheNames.RELATED);
        clearCache(CacheNames.FBT);
        clearCache(CacheNames.TRENDING);
    }

    @Override
    public void evictAllOnBookDelete() {
        clearAllRecSysCaches();
    }

    @Override
    public void clearAllRecSysCaches() {
        for (String cacheName : ALL_RECSYS_CACHES) {
            clearCache(cacheName);
        }
    }

    private void clearCache(String cacheName) {
        if (cacheManager == null) {
            return;
        }
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                if (cache instanceof RedisCache redisCache) {
                    redisCache.invalidate();
                } else {
                    cache.clear();
                }
                log.info("🧹 [CacheEviction] Đã làm sạch cache: {}", cacheName);
            }
        } catch (Exception e) {
            log.warn("⚠️ [CacheEviction] Không thể làm sạch cache [{}]: {}", cacheName, e.getMessage());
        }
    }
}
