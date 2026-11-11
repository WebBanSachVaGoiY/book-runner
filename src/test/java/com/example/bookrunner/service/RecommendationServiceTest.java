package com.example.bookrunner.service;

import com.example.bookrunner.config.RecSysProperties;
import com.example.bookrunner.dto.response.BookResponseDTO;
import com.example.bookrunner.enums.RecommendationType;
import com.example.bookrunner.model.Book;
import com.example.bookrunner.model.BookSimilarity;
import com.example.bookrunner.model.Category;
import com.example.bookrunner.model.UserRecommendation;
import com.example.bookrunner.repository.BookRepository;
import com.example.bookrunner.repository.BookSimilarityRepository;
import com.example.bookrunner.repository.UserBookInteractionRepository;
import com.example.bookrunner.repository.UserRecommendationRepository;
import com.example.bookrunner.service.impl.RecommendationServiceImpl;
import com.example.bookrunner.service.mapper.BookMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private UserRecommendationRepository userRecommendationRepository;

    @Mock
    private BookSimilarityRepository bookSimilarityRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserBookInteractionRepository interactionRepository;

    @Mock
    private BookMapper bookMapper;

    private RecommendationServiceImpl recommendationService;

    @BeforeEach
    void setUp() {
        RecSysProperties props = new RecSysProperties();
        props.setDefaultRecommendationLimit(10);
        props.setMaxRecommendationLimit(50);
        props.setUserColdStartThreshold(5);
        recommendationService = new RecommendationServiceImpl(
                userRecommendationRepository,
                bookSimilarityRepository,
                bookRepository,
                interactionRepository,
                bookMapper,
                props);
    }

    // ------------------------------------------------------------------
    // getForYou
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getForYou - giữ đúng thứ hạng và bỏ sách inactive")
    void getForYou_preservesRankAndFiltersInactive() {
        when(interactionRepository.countByUserId(1L)).thenReturn(10L);
        when(userRecommendationRepository.findByUserIdAndRecommendationTypeOrderByRankOrderAsc(
                eq(1L), eq(RecommendationType.FOR_YOU), any(Pageable.class)))
                .thenReturn(List.of(rec(5L, 1), rec(6L, 2), rec(7L, 3)));
        // Sách id=5 đã bị ẩn/không tồn tại → chỉ còn 6, 7
        when(bookRepository.findAllActiveByIdInWithCategory(anyList()))
                .thenReturn(List.of(book(6L), book(7L)));
        stubToDTO();

        List<BookResponseDTO> result = recommendationService.getForYou(1L, 10);

        assertEquals(List.of(6L, 7L), ids(result));
    }

    @Test
    @DisplayName("getForYou - user cold-start trả trending, không đọc user_recommendations")
    void getForYou_coldStart_returnsTrending() {
        when(interactionRepository.countByUserId(2L)).thenReturn(1L);
        when(bookRepository.findTrendingWithCategory(any(Pageable.class))).thenReturn(List.of(book(10L)));
        when(bookRepository.findTopRatedWithCategory(anyDouble(), any(Pageable.class))).thenReturn(List.of());
        stubToDTO();

        List<BookResponseDTO> result = recommendationService.getForYou(2L, 5);

        assertEquals(List.of(10L), ids(result));
        verify(userRecommendationRepository, never())
                .findByUserIdAndRecommendationTypeOrderByRankOrderAsc(any(), any(), any());
    }

    @Test
    @DisplayName("getForYou - chưa có gợi ý thì fallback trending")
    void getForYou_emptyRecommendations_fallsBackToTrending() {
        when(interactionRepository.countByUserId(3L)).thenReturn(50L);
        when(userRecommendationRepository.findByUserIdAndRecommendationTypeOrderByRankOrderAsc(
                eq(3L), eq(RecommendationType.FOR_YOU), any(Pageable.class)))
                .thenReturn(List.of());
        when(bookRepository.findTrendingWithCategory(any(Pageable.class))).thenReturn(List.of(book(11L)));
        when(bookRepository.findTopRatedWithCategory(anyDouble(), any(Pageable.class))).thenReturn(List.of());
        stubToDTO();

        List<BookResponseDTO> result = recommendationService.getForYou(3L, 5);

        assertEquals(List.of(11L), ids(result));
    }

    @Test
    @DisplayName("getForYou - userId null trả trending")
    void getForYou_nullUser_returnsTrending() {
        when(bookRepository.findTrendingWithCategory(any(Pageable.class))).thenReturn(List.of(book(12L)));
        when(bookRepository.findTopRatedWithCategory(anyDouble(), any(Pageable.class))).thenReturn(List.of());
        stubToDTO();

        List<BookResponseDTO> result = recommendationService.getForYou(null, 5);

        assertEquals(List.of(12L), ids(result));
    }

    // ------------------------------------------------------------------
    // getRelated / getFrequentlyBoughtTogether
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getRelated - không có dữ liệu tương đồng thì fallback cùng danh mục")
    void getRelated_fallsBackToSameCategory_whenNoSimilarity() {
        when(bookSimilarityRepository.findByBookId1AndSimilarityTypeOrderByRankOrderAsc(
                eq(20L), eq("CONTENT_BASED"), any(Pageable.class)))
                .thenReturn(List.of());
        Category category = Category.builder().id(2L).name("Tech").slug("tech").build();
        Book source = Book.builder().id(20L).title("Source").category(category).build();
        when(bookRepository.findByIdWithCategory(20L)).thenReturn(Optional.of(source));
        when(bookRepository.findByCategoryExcludingWithCategory(eq(2L), eq(20L), any(Pageable.class)))
                .thenReturn(List.of(book(21L), book(22L)));
        when(bookMapper.toDTOList(anyList())).thenAnswer(inv -> {
            List<Book> books = inv.getArgument(0);
            return books.stream().map(this::dto).toList();
        });

        List<BookResponseDTO> result = recommendationService.getRelated(20L, 10);

        assertEquals(List.of(21L, 22L), ids(result));
    }

    @Test
    @DisplayName("getRelated - không có danh mục và không có tương đồng thì trả rỗng (không lỗi)")
    void getRelated_noCategory_returnsEmpty() {
        when(bookSimilarityRepository.findByBookId1AndSimilarityTypeOrderByRankOrderAsc(
                eq(23L), eq("CONTENT_BASED"), any(Pageable.class)))
                .thenReturn(List.of());
        Book source = Book.builder().id(23L).title("NoCat").build();
        when(bookRepository.findByIdWithCategory(23L)).thenReturn(Optional.of(source));

        List<BookResponseDTO> result = recommendationService.getRelated(23L, 10);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getFrequentlyBoughtTogether - dùng đúng similarity_type ALSO_BOUGHT")
    void getFrequentlyBoughtTogether_usesAlsoBoughtType() {
        when(bookSimilarityRepository.findByBookId1AndSimilarityTypeOrderByRankOrderAsc(
                eq(30L), eq("ALSO_BOUGHT"), any(Pageable.class)))
                .thenReturn(List.of(sim(31L, 1), sim(32L, 2)));
        when(bookRepository.findAllActiveByIdInWithCategory(anyList()))
                .thenReturn(List.of(book(31L), book(32L)));
        stubToDTO();

        List<BookResponseDTO> result = recommendationService.getFrequentlyBoughtTogether(30L, 10);

        assertEquals(List.of(31L, 32L), ids(result));
        verify(bookSimilarityRepository).findByBookId1AndSimilarityTypeOrderByRankOrderAsc(
                eq(30L), eq("ALSO_BOUGHT"), any(Pageable.class));
    }

    @Test
    @DisplayName("getFrequentlyBoughtTogether - không có dữ liệu ALSO_BOUGHT thì trả rỗng không fallback danh mục")
    void getFrequentlyBoughtTogether_noSimilarity_returnsEmptyWithoutFallback() {
        when(bookSimilarityRepository.findByBookId1AndSimilarityTypeOrderByRankOrderAsc(
                eq(40L), eq("ALSO_BOUGHT"), any(Pageable.class)))
                .thenReturn(List.of());

        List<BookResponseDTO> result = recommendationService.getFrequentlyBoughtTogether(40L, 10);

        assertTrue(result.isEmpty());
        verify(bookRepository, never()).findByIdWithCategory(anyLong());
        verify(bookRepository, never()).findByCategoryExcludingWithCategory(anyLong(), anyLong(), any(Pageable.class));
    }

    // ------------------------------------------------------------------
    // getTrending
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getTrending - gộp bán chạy + điểm cao, loại trùng, giữ thứ tự")
    void getTrending_mergesAndDeduplicates() {
        when(bookRepository.findTrendingWithCategory(any(Pageable.class)))
                .thenReturn(List.of(book(1L), book(2L)));
        when(bookRepository.findTopRatedWithCategory(anyDouble(), any(Pageable.class)))
                .thenReturn(List.of(book(2L), book(3L)));
        stubToDTO();

        List<BookResponseDTO> result = recommendationService.getTrending(10);

        assertEquals(List.of(1L, 2L, 3L), ids(result));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void stubToDTO() {
        when(bookMapper.toDTO(any(Book.class))).thenAnswer(inv -> dto(inv.getArgument(0)));
    }

    private BookResponseDTO dto(Book book) {
        BookResponseDTO dto = new BookResponseDTO();
        dto.setId(book.getId());
        return dto;
    }

    private List<Long> ids(List<BookResponseDTO> list) {
        return list.stream().map(BookResponseDTO::getId).toList();
    }

    private Book book(Long id) {
        return Book.builder().id(id).title("Book " + id).build();
    }

    private UserRecommendation rec(Long bookId, int rank) {
        return UserRecommendation.builder()
                .userId(1L)
                .bookId(bookId)
                .recommendationType(RecommendationType.FOR_YOU)
                .algorithm("ALS")
                .score(1f)
                .rankOrder(rank)
                .build();
    }

    private BookSimilarity sim(Long bookId2, int rank) {
        return BookSimilarity.builder()
                .bookId1(0L)
                .bookId2(bookId2)
                .similarityType("ALSO_BOUGHT")
                .score(1f)
                .rankOrder(rank)
                .build();
    }
}
