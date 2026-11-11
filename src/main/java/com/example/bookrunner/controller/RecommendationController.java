package com.example.bookrunner.controller;

import com.example.bookrunner.dto.common.ApiResponse;
import com.example.bookrunner.dto.response.BookResponseDTO;
import com.example.bookrunner.security.CustomUserDetails;
import com.example.bookrunner.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint phục vụ gợi ý cho Frontend.
 * Quyền truy cập được cấu hình trong SecurityConfig:
 * - /for-you            → authenticated
 * - /related/**, /trending, /frequently-bought-together/** → permitAll
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/for-you")
    public ResponseEntity<ApiResponse<List<BookResponseDTO>>> getForYou(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        List<BookResponseDTO> recommendations = recommendationService.getForYou(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách gợi ý dành cho bạn thành công", recommendations));
    }

    @GetMapping("/related/{bookId}")
    public ResponseEntity<ApiResponse<List<BookResponseDTO>>> getRelated(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "10") int limit) {
        List<BookResponseDTO> recommendations = recommendationService.getRelated(bookId, limit);
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách sách tương tự thành công", recommendations));
    }

    @GetMapping("/frequently-bought-together/{bookId}")
    public ResponseEntity<ApiResponse<List<BookResponseDTO>>> getFrequentlyBoughtTogether(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "10") int limit) {
        List<BookResponseDTO> recommendations = recommendationService.getFrequentlyBoughtTogether(bookId, limit);
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách sách thường được mua cùng thành công", recommendations));
    }

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<BookResponseDTO>>> getTrending(
            @RequestParam(defaultValue = "10") int limit) {
        List<BookResponseDTO> recommendations = recommendationService.getTrending(limit);
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách sách bán chạy thành công", recommendations));
    }
}
