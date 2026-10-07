package com.example.bookrunner.service;

import com.example.bookrunner.dto.request.CreateReviewRequest;
import com.example.bookrunner.dto.request.UpdateReviewRequest;
import com.example.bookrunner.dto.response.CanReviewResponseDTO;
import com.example.bookrunner.dto.response.ReviewResponseDTO;
import org.springframework.data.domain.Page;

public interface ReviewService {

    Page<ReviewResponseDTO> getReviewsByBook(Long bookId, int page, int size);

    ReviewResponseDTO createReview(Long userId, Long bookId, CreateReviewRequest request);

    ReviewResponseDTO updateReview(Long userId, Long reviewId, UpdateReviewRequest request);

    void deleteReview(Long userId, Long reviewId, boolean isAdmin);

    CanReviewResponseDTO canUserReview(Long userId, Long bookId);
}
