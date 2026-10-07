package com.example.bookrunner.service.impl;

import com.example.bookrunner.dto.request.CreateReviewRequest;
import com.example.bookrunner.dto.request.UpdateReviewRequest;
import com.example.bookrunner.dto.response.CanReviewResponseDTO;
import com.example.bookrunner.dto.response.ReviewResponseDTO;
import com.example.bookrunner.enums.InteractionType;
import com.example.bookrunner.enums.Role;
import com.example.bookrunner.exception.BadRequestException;
import com.example.bookrunner.exception.ItemNotFoundException;
import com.example.bookrunner.model.Book;
import com.example.bookrunner.model.Review;
import com.example.bookrunner.model.User;
import com.example.bookrunner.model.UserBookInteraction;
import com.example.bookrunner.repository.*;
import com.example.bookrunner.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final UserBookInteractionRepository interactionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponseDTO> getReviewsByBook(Long bookId, int page, int size) {
        if (!bookRepository.existsById(bookId)) {
            throw new ItemNotFoundException("Không tìm thấy sách với id: " + bookId);
        }
        Pageable pageable = PageRequest.of(page, size);
        return reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public CanReviewResponseDTO canUserReview(Long userId, Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new ItemNotFoundException("Không tìm thấy sách với id: " + bookId);
        }

        boolean alreadyReviewed = Boolean.TRUE.equals(reviewRepository.existsByUserIdAndBookId(userId, bookId));
        if (alreadyReviewed) {
            return CanReviewResponseDTO.builder()
                    .canReview(false)
                    .alreadyReviewed(true)
                    .hasPurchased(true)
                    .reason("Bạn đã đánh giá cuốn sách này rồi.")
                    .build();
        }

        boolean hasPurchased = orderRepository.hasUserPurchasedBookAndDelivered(userId, bookId);
        User user = userRepository.findById(userId).orElse(null);
        boolean isAdmin = user != null && user.getRole() == Role.ROLE_ADMIN;

        if (!hasPurchased && !isAdmin) {
            return CanReviewResponseDTO.builder()
                    .canReview(false)
                    .alreadyReviewed(false)
                    .hasPurchased(false)
                    .reason("Bạn cần mua và nhận sách thành công trước khi viết đánh giá.")
                    .build();
        }

        return CanReviewResponseDTO.builder()
                .canReview(true)
                .alreadyReviewed(false)
                .hasPurchased(true)
                .reason(null)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewResponseDTO createReview(Long userId, Long bookId, CreateReviewRequest request) {
        Long targetBookId = (bookId != null) ? bookId : request.getBookId();
        if (targetBookId == null) {
            throw new BadRequestException("Mã sách (bookId) không được để trống!");
        }

        Book book = bookRepository.findById(targetBookId)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy sách với id: " + targetBookId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy người dùng với id: " + userId));

        if (Boolean.TRUE.equals(reviewRepository.existsByUserIdAndBookId(userId, targetBookId))) {
            throw new BadRequestException("Bạn đã đánh giá cuốn sách này rồi!");
        }

        boolean hasPurchased = orderRepository.hasUserPurchasedBookAndDelivered(userId, targetBookId);
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;
        if (!hasPurchased && !isAdmin) {
            throw new BadRequestException("Bạn chỉ có thể đánh giá sách sau khi đã mua và nhận hàng thành công!");
        }

        Review review = Review.builder()
                .user(user)
                .book(book)
                .rating(request.getRating())
                .comment(request.getComment() != null ? request.getComment().trim() : "")
                .build();

        Review savedReview = reviewRepository.save(review);

        // 1. Cập nhật lại averageRating và totalReviews của sách
        updateBookRatingStats(book);

        // 2. Ghi nhận tương tác RATING vào bảng user_book_interactions cho RecSys
        try {
            UserBookInteraction interaction = UserBookInteraction.builder()
                    .user(user)
                    .book(book)
                    .interactionType(InteractionType.RATING)
                    .weight((float) request.getRating())
                    .explicitRating(request.getRating())
                    .build();
            interactionRepository.save(interaction);
        } catch (Exception e) {
            log.warn("Không thể lưu interaction RATING cho user {} và book {}: {}", userId, targetBookId, e.getMessage());
        }

        log.info("Người dùng '{}' đã đánh giá sách '{}' {} sao", user.getUsername(), book.getTitle(), request.getRating());
        return mapToDTO(savedReview);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewResponseDTO updateReview(Long userId, Long reviewId, UpdateReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy đánh giá với id: " + reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa đánh giá này!");
        }

        review.setRating(request.getRating());
        if (request.getComment() != null) {
            review.setComment(request.getComment().trim());
        }

        Review updatedReview = reviewRepository.save(review);
        updateBookRatingStats(review.getBook());

        log.info("Người dùng '{}' đã cập nhật đánh giá ID {}", review.getUser().getUsername(), reviewId);
        return mapToDTO(updatedReview);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(Long userId, Long reviewId, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy đánh giá với id: " + reviewId));

        if (!review.getUser().getId().equals(userId) && !isAdmin) {
            throw new AccessDeniedException("Bạn không có quyền xóa đánh giá này!");
        }

        Book book = review.getBook();
        reviewRepository.delete(review);
        updateBookRatingStats(book);

        log.info("Đã xóa đánh giá ID {} bởi user ID {}", reviewId, userId);
    }

    private void updateBookRatingStats(Book book) {
        long total = reviewRepository.countByBookId(book.getId());
        Double avg = reviewRepository.getAverageRatingByBookId(book.getId());
        book.setTotalReviews((int) total);
        book.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
        bookRepository.save(book);
    }

    private ReviewResponseDTO mapToDTO(Review review) {
        return ReviewResponseDTO.builder()
                .id(review.getId())
                .bookId(review.getBook().getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .user(ReviewResponseDTO.ReviewUserDTO.builder()
                        .id(review.getUser().getId())
                        .username(review.getUser().getUsername())
                        .fullName(review.getUser().getFullName() != null ? review.getUser().getFullName() : review.getUser().getUsername())
                        .build())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
