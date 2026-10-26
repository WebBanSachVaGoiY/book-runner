package com.example.bookrunner.scheduler;

import com.example.bookrunner.config.RecSysProperties;
import com.example.bookrunner.service.RecSysAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Tác vụ định kỳ tự động kích hoạt huấn luyện lại RecSys Microservice
 * và xóa cache gợi ý để dữ liệu phục vụ luôn mới nhất.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecSysScheduledTrainer {

    private final RecSysAdminService recSysAdminService;
    private final RecSysProperties recSysProperties;

    @Scheduled(cron = "${app.recsys.training-cron:0 0 2 * * ?}")
    public void scheduledRetrain() {
        if (!recSysProperties.isAutoTrainingEnabled()) {
            log.info("[RecSysScheduler] Tự động huấn luyện đang bị tắt qua cấu hình (autoTrainingEnabled=false).");
            return;
        }

        log.info("[RecSysScheduler] Bắt đầu tác vụ định kỳ tự động huấn luyện RecSys (cron: {})...",
                recSysProperties.getTrainingCron());
        try {
            Map<String, Object> response = recSysAdminService.train("all");
            log.info("[RecSysScheduler] Tự động huấn luyện thành công! Kết quả: {}", response);
        } catch (Exception e) {
            log.warn("[RecSysScheduler] Tự động huấn luyện RecSys thất bại (Microservice FastAPI có thể đang ngoại tuyến): {}",
                    e.getMessage());
        }
    }
}
