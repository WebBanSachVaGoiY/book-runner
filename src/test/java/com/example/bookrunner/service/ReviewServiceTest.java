package com.example.bookrunner.service;

import com.example.bookrunner.dto.request.CreateReviewRequest;
import com.example.bookrunner.dto.request.UpdateReviewRequest;
import com.example.bookrunner.dto.response.CanReviewResponseDTO;
import com.example.bookrunner.dto.response.ReviewResponseDTO;
import com.example.bookrunner.exception.BadRequestException;
import com.example.bookrunner.model.Book;
import com.example.bookrunner.model.Review;
import com.example.bookrunner.model.User;
import com.example.bookrunner.repository.BookRepository;
import com.example.bookrunner.repository.OrderRepository;
import com.example.bookrunner.repository.ReviewRepository;
import com.example.bookrunner.repository.UserBookInteractionRepository;
import com.example.bookrunner.repository.UserRepository;
import com.example.bookrunner.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserBookInteractionRepository interactionRepository;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User testUser;
    private Book testBook;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .fullName("Test User")
                .email("test@example.com")
                .build();

        testBook = Book.builder()
                .id(10L)
                .title("Dế Mèn Phiêu Lưu Ký")
                .price(new BigDecimal("50000"))
                .stockQuantity(100)
                .active(true)
                .averageRating(0.0)
                .totalReviews(0)
                .build();
    }

    @Test
    @DisplayName("canUserReview - Chưa mua hàng thì không được đánh giá")
    void canUserReview_NotPurchased() {
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(reviewRepository.existsByUserIdAndBookId(1L, 10L)).thenReturn(false);
        when(orderRepository.hasUserPurchasedBookAndDelivered(1L, 10L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        CanReviewResponseDTO result = reviewService.canUserReview(1L, 10L);

        assertFalse(result.isCanReview());
        assertFalse(result.isHasPurchased());
        assertFalse(result.isAlreadyReviewed());
        assertNotNull(result.getReason());
    }

    @Test
    @DisplayName("canUserReview - Đã mua và đơn hàng DELIVERED thì được phép đánh giá")
    void canUserReview_PurchasedAndDelivered() {
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(reviewRepository.existsByUserIdAndBookId(1L, 10L)).thenReturn(false);
        when(orderRepository.hasUserPurchasedBookAndDelivered(1L, 10L)).thenReturn(true);

        CanReviewResponseDTO result = reviewService.canUserReview(1L, 10L);

        assertTrue(result.isCanReview());
        assertTrue(result.isHasPurchased());
        assertFalse(result.isAlreadyReviewed());
    }

    @Test
    @DisplayName("canUserReview - Đã đánh giá rồi thì không được tạo mới nữa")
    void canUserReview_AlreadyReviewed() {
        when(bookRepository.existsById(10L)).thenReturn(true);
        when(reviewRepository.existsByUserIdAndBookId(1L, 10L)).thenReturn(true);

        CanReviewResponseDTO result = reviewService.canUserReview(1L, 10L);

        assertFalse(result.isCanReview());
        assertTrue(result.isAlreadyReviewed());
    }

    @Test
    @DisplayName("createReview - Tạo đánh giá thành công khi đủ điều kiện")
    void createReview_Success() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setBookId(10L);
        request.setRating(5);
        request.setComment("Sách rất hay!");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.existsByUserIdAndBookId(1L, 10L)).thenReturn(false);
        when(orderRepository.hasUserPurchasedBookAndDelivered(1L, 10L)).thenReturn(true);

        Review saved = Review.builder()
                .id(100L)
                .user(testUser)
                .book(testBook)
                .rating(5)
                .comment("Sách rất hay!")
                .build();
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);
        when(reviewRepository.countByBookId(10L)).thenReturn(1L);
        when(reviewRepository.getAverageRatingByBookId(10L)).thenReturn(5.0);

        ReviewResponseDTO response = reviewService.createReview(1L, 10L, request);

        assertNotNull(response);
        assertEquals(5, response.getRating());
        assertEquals("Sách rất hay!", response.getComment());
        verify(bookRepository).save(testBook);
        verify(interactionRepository).save(any());
    }

    @Test
    @DisplayName("createReview - Ném BadRequestException nếu chưa từng mua sách")
    void createReview_ThrowsIfNotPurchased() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setBookId(10L);
        request.setRating(5);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.existsByUserIdAndBookId(1L, 10L)).thenReturn(false);
        when(orderRepository.hasUserPurchasedBookAndDelivered(1L, 10L)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> reviewService.createReview(1L, 10L, request));
    }
}
