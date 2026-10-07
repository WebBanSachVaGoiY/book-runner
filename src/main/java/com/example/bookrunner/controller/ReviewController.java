package com.example.bookrunner.controller;

import com.example.bookrunner.dto.common.ApiResponse;
import com.example.bookrunner.dto.request.CreateReviewRequest;
import com.example.bookrunner.dto.request.UpdateReviewRequest;
import com.example.bookrunner.dto.response.CanReviewResponseDTO;
import com.example.bookrunner.dto.response.ReviewResponseDTO;
import com.example.bookrunner.security.CustomUserDetails;
import com.example.bookrunner.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/books/{bookId}/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponseDTO>>> getReviewsByBook(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ReviewResponseDTO> reviews = reviewService.getReviewsByBook(bookId, page, size);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/books/{bookId}/can-review")
    public ResponseEntity<ApiResponse<CanReviewResponseDTO>> canUserReview(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long bookId) {
        CanReviewResponseDTO result = reviewService.canUserReview(currentUser.getId(), bookId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/books/{bookId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponseDTO>> createReview(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long bookId,
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponseDTO response = reviewService.createReview(currentUser.getId(), bookId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đánh giá sản phẩm thành công", response));
    }

    @PutMapping("/reviews/{id}")
    public ResponseEntity<ApiResponse<ReviewResponseDTO>> updateReview(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request) {
        ReviewResponseDTO response = reviewService.updateReview(currentUser.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật đánh giá thành công", response));
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        reviewService.deleteReview(currentUser.getId(), id, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Xóa đánh giá thành công", null));
    }
}
