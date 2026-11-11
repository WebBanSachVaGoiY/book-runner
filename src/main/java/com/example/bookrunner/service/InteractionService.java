package com.example.bookrunner.service;

import com.example.bookrunner.dto.request.InteractionLogRequest;

public interface InteractionService {

    /**
     * Ghi nhận tương tác bất đồng bộ qua Worker ThreadPool (Non-blocking, Fire-and-forget).
     */
    void logInteractionAsync(Long userId, InteractionLogRequest request);

    /**
     * Ghi nhận tương tác đồng bộ (dùng nội bộ hoặc Unit Test).
     */
    void logInteraction(Long userId, InteractionLogRequest request);

    /**
     * Xóa cache debounce (phục vụ Unit Test hoặc bảo trì).
     */
    void clearDebounceCache();
}
