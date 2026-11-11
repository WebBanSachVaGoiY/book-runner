package com.example.bookrunner.service;

import com.example.bookrunner.config.RecSysProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * Client gọi RecSys Microservice (FastAPI) để trigger huấn luyện mô hình.
 * Chỉ dùng khi admin bấm nút train — tầng phục vụ gợi ý KHÔNG phụ thuộc client này.
 */
@Slf4j
@Component
public class RecSysTrainingClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    /** Training (đặc biệt ALS) có thể chạy lâu → đặt read timeout rộng. */
    private static final Duration READ_TIMEOUT = Duration.ofMinutes(10);

    private final RestClient restClient;

    public RecSysTrainingClient(RecSysProperties recSysProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder()
                .baseUrl(recSysProperties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
        log.info("RecSysTrainingClient trỏ tới: {}", recSysProperties.getBaseUrl());
    }

    /**
     * Trigger training trên FastAPI.
     *
     * @param type một trong {@code content-based}, {@code collaborative}, {@code co-purchase}, {@code all}
     * @return payload JSON trả về từ FastAPI
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> train(String type) {
        return restClient.post()
                .uri("/api/recsys/train/{type}", type)
                .retrieve()
                .body(Map.class);
    }

    /**
     * Lấy chỉ số đánh giá mô hình RecSys từ FastAPI.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> evaluate(int topK) {
        return restClient.get()
                .uri("/api/recsys/evaluate?top_k={topK}", topK)
                .retrieve()
                .body(Map.class);
    }
}
