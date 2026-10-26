package com.example.bookrunner.service.impl;

import com.example.bookrunner.config.CacheNames;
import com.example.bookrunner.exception.BadRequestException;
import com.example.bookrunner.service.RecSysAdminService;
import com.example.bookrunner.service.RecSysTrainingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecSysAdminServiceImpl implements RecSysAdminService {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("content-based", "collaborative", "co-purchase", "hybrid", "all");

    private static final List<String> RECOMMENDATION_CACHES = List.of(
            CacheNames.FOR_YOU, CacheNames.RELATED, CacheNames.FBT, CacheNames.TRENDING);

    private final RecSysTrainingClient recSysTrainingClient;
    private final CacheManager cacheManager;

    @Override
    public Map<String, Object> train(String type) {
        String safeType = (type == null || type.isBlank())
                ? "all"
                : type.trim().toLowerCase(Locale.ROOT);

        if (!ALLOWED_TYPES.contains(safeType)) {
            throw new BadRequestException(
                    "Loại training không hợp lệ: '" + type + "'. Chỉ chấp nhận: " + ALLOWED_TYPES);
        }

        log.info("Admin trigger RecSys training: type={}", safeType);
        Map<String, Object> result = recSysTrainingClient.train(safeType);

        evictRecommendationCaches();
        return result;
    }

    @Override
    public Map<String, Object> evaluate(int topK) {
        int safeK = (topK <= 0) ? 10 : Math.min(topK, 50);
        log.info("Admin yêu cầu đo lường đánh giá mô hình RecSys: topK={}", safeK);
        return recSysTrainingClient.evaluate(safeK);
    }

    private void evictRecommendationCaches() {
        for (String cacheName : RECOMMENDATION_CACHES) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.info("Đã xóa cache gợi ý: {}", cacheName);
            }
        }
    }
}
