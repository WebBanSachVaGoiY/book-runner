package com.example.bookrunner.service.impl;

import com.example.bookrunner.exception.BadRequestException;
import com.example.bookrunner.service.RecSysAdminService;
import com.example.bookrunner.service.RecSysCacheService;
import com.example.bookrunner.service.RecSysTrainingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecSysAdminServiceImpl implements RecSysAdminService {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("content-based", "collaborative", "co-purchase", "hybrid", "all");

    private final RecSysTrainingClient recSysTrainingClient;
    private final RecSysCacheService recSysCacheService;

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

        recSysCacheService.clearAllRecSysCaches();
        return result;
    }

    @Override
    public Map<String, Object> evaluate(int topK) {
        int safeK = (topK <= 0) ? 10 : Math.min(topK, 50);
        log.info("Admin yêu cầu đo lường đánh giá mô hình RecSys: topK={}", safeK);
        return recSysTrainingClient.evaluate(safeK);
    }
}
