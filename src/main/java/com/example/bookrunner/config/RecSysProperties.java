package com.example.bookrunner.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình cho tầng gợi ý (Recommendation Serving Layer).
 * Bind từ các key {@code app.recsys.*} trong application.yaml.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.recsys")
public class RecSysProperties {

    /** Số tương tác tối thiểu để coi user là "đủ dữ liệu" (dưới ngưỡng → cold-start). */
    private int userColdStartThreshold = 5;

    /** Số tương tác tối thiểu của một sách (dùng cho item cold-start). */
    private int itemColdStartThreshold = 5;

    /** Số gợi ý mặc định khi client không truyền limit. */
    private int defaultRecommendationLimit = 10;

    /** Giới hạn cứng số gợi ý trả về trong một request. */
    private int maxRecommendationLimit = 50;

    /** TTL (giờ) cho cache gợi ý cá nhân hoá (for-you). Các cache khác có TTL cố định. */
    private long cacheTtlHours = 2;

    /** Base URL của RecSys Microservice (FastAPI) để trigger training. */
    private String baseUrl = "http://localhost:8000";

    /** Biểu thức Cron tự động huấn luyện lại RecSys (mặc định 2:00 sáng hàng ngày). */
    private String trainingCron = "0 0 2 * * ?";

    /** Cờ bật/tắt tác vụ tự động huấn luyện theo lịch. */
    private boolean autoTrainingEnabled = true;
}
