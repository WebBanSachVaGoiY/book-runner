package com.example.bookrunner.service;

import com.example.bookrunner.dto.response.BookResponseDTO;

import java.util.List;

/**
 * Tầng phục vụ gợi ý — đọc kết quả đã huấn luyện từ 2 bảng
 * {@code user_recommendations} (ALS) và {@code book_similarities} (CBF / Co-Purchase).
 */
public interface RecommendationService {

    /**
     * Gợi ý cá nhân hoá "Dành riêng cho bạn" (FOR_YOU).
     * Tự động fallback sang trending khi user cold-start hoặc chưa có dữ liệu.
     *
     * @param userId id người dùng (có thể null → trả trending)
     * @param limit  số gợi ý tối đa
     */
    List<BookResponseDTO> getForYou(Long userId, int limit);

    /**
     * Sách tương tự theo nội dung (Content-Based).
     * Fallback sang sách cùng danh mục khi chưa có dữ liệu tương đồng.
     */
    List<BookResponseDTO> getRelated(Long bookId, int limit);

    /**
     * Sách "thường được mua cùng" (Item-Item Co-Purchase).
     * Fallback sang sách cùng danh mục khi chưa có dữ liệu.
     */
    List<BookResponseDTO> getFrequentlyBoughtTogether(Long bookId, int limit);

    /**
     * Sách bán chạy / điểm cao (dùng cho khách vãng lai và cold-start).
     */
    List<BookResponseDTO> getTrending(int limit);
}
