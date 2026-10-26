package com.example.bookrunner.service.impl;

import com.example.bookrunner.config.CacheNames;
import com.example.bookrunner.config.RecSysProperties;
import com.example.bookrunner.dto.response.BookResponseDTO;
import com.example.bookrunner.enums.RecommendationType;
import com.example.bookrunner.model.Book;
import com.example.bookrunner.model.BookSimilarity;
import com.example.bookrunner.model.UserRecommendation;
import com.example.bookrunner.repository.BookRepository;
import com.example.bookrunner.repository.BookSimilarityRepository;
import com.example.bookrunner.repository.UserBookInteractionRepository;
import com.example.bookrunner.repository.UserRecommendationRepository;
import com.example.bookrunner.service.RecommendationService;
import com.example.bookrunner.service.mapper.BookMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final String SIMILARITY_CONTENT_BASED = "CONTENT_BASED";
    private static final String SIMILARITY_ALSO_BOUGHT = "ALSO_BOUGHT";
    private static final Double MIN_TRENDING_RATING = 4.0;

    /** Lấy dư bản ghi để sau khi lọc sách inactive vẫn đủ số lượng yêu cầu. */
    private static final int MAX_FETCH = 100;

    private final UserRecommendationRepository userRecommendationRepository;
    private final BookSimilarityRepository bookSimilarityRepository;
    private final BookRepository bookRepository;
    private final UserBookInteractionRepository interactionRepository;
    private final BookMapper bookMapper;
    private final RecSysProperties recSysProperties;

    // ------------------------------------------------------------------
    // FOR YOU
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.FOR_YOU, key = "#userId + ':' + #limit")
    public List<BookResponseDTO> getForYou(Long userId, int limit) {
        int safeLimit = clampLimit(limit);

        if (userId == null) {
            return getTrending(safeLimit);
        }

        // Cold-start: user có quá ít tương tác → chưa đủ dữ liệu để cá nhân hoá
        long interactionCount = interactionRepository.countByUserId(userId);
        if (interactionCount < recSysProperties.getUserColdStartThreshold()) {
            log.debug("User {} cold-start ({} tương tác < {}), trả trending.",
                    userId, interactionCount, recSysProperties.getUserColdStartThreshold());
            return getTrending(safeLimit);
        }

        List<UserRecommendation> recommendations = userRecommendationRepository
                .findByUserIdAndRecommendationTypeOrderByRankOrderAsc(
                        userId, RecommendationType.FOR_YOU, PageRequest.of(0, MAX_FETCH));

        List<BookResponseDTO> result = mapRecommendationsToBooks(recommendations, safeLimit);
        if (result.isEmpty()) {
            // Chưa train hoặc tất cả sách gợi ý đã bị ẩn → fallback trending
            return getTrending(safeLimit);
        }
        return result;
    }

    // ------------------------------------------------------------------
    // RELATED (CONTENT_BASED) & FREQUENTLY BOUGHT TOGETHER (ALSO_BOUGHT)
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.RELATED, key = "#bookId + ':' + #limit")
    public List<BookResponseDTO> getRelated(Long bookId, int limit) {
        return getSimilarityBooks(bookId, SIMILARITY_CONTENT_BASED, clampLimit(limit), true);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.FBT, key = "#bookId + ':' + #limit")
    public List<BookResponseDTO> getFrequentlyBoughtTogether(Long bookId, int limit) {
        return getSimilarityBooks(bookId, SIMILARITY_ALSO_BOUGHT, clampLimit(limit), false);
    }

    // ------------------------------------------------------------------
    // TRENDING (cold-start / khách vãng lai)
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.TRENDING, key = "#limit")
    public List<BookResponseDTO> getTrending(int limit) {
        int safeLimit = clampLimit(limit);

        List<Book> bestSellers = bookRepository.findTrendingWithCategory(PageRequest.of(0, safeLimit));
        List<Book> topRated = bookRepository.findTopRatedWithCategory(MIN_TRENDING_RATING,
                PageRequest.of(0, safeLimit));

        // Merge: ưu tiên bán chạy trước, bổ sung sách điểm cao chưa xuất hiện
        LinkedHashMap<Long, Book> merged = new LinkedHashMap<>();
        bestSellers.forEach(b -> merged.putIfAbsent(b.getId(), b));
        topRated.forEach(b -> merged.putIfAbsent(b.getId(), b));

        return merged.values().stream()
                .limit(safeLimit)
                .map(bookMapper::toDTO)
                .toList();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private List<BookResponseDTO> getSimilarityBooks(Long bookId, String similarityType, int limit, boolean enableFallback) {
        if (bookId == null) {
            return List.of();
        }
        List<BookSimilarity> similarities = bookSimilarityRepository
                .findByBookId1AndSimilarityTypeOrderByRankOrderAsc(
                        bookId, similarityType, PageRequest.of(0, MAX_FETCH));

        List<BookResponseDTO> result = mapSimilarityToBooks(similarities, limit);
        if (result.isEmpty() && enableFallback) {
            // Item cold-start: chưa có dữ liệu tương đồng → gợi ý cùng danh mục
            result = getCategoryFallback(bookId, limit);
        }
        return result;
    }

    private List<BookResponseDTO> mapRecommendationsToBooks(List<UserRecommendation> recommendations, int limit) {
        if (recommendations.isEmpty()) {
            return List.of();
        }
        List<Long> bookIds = recommendations.stream()
                .map(UserRecommendation::getBookId)
                .toList();
        Map<Long, Book> booksById = fetchActiveBooksById(bookIds);
        return collectInOrder(
                recommendations.stream().map(UserRecommendation::getBookId).collect(Collectors.toList()),
                booksById, limit);
    }

    private List<BookResponseDTO> mapSimilarityToBooks(List<BookSimilarity> similarities, int limit) {
        if (similarities.isEmpty()) {
            return List.of();
        }
        List<Long> bookIds = similarities.stream()
                .map(BookSimilarity::getBookId2)
                .toList();
        Map<Long, Book> booksById = fetchActiveBooksById(bookIds);
        return collectInOrder(
                similarities.stream().map(BookSimilarity::getBookId2).collect(Collectors.toList()),
                booksById, limit);
    }

    /**
     * Giữ đúng thứ hạng gợi ý sau khi lọc bỏ sách inactive/không tồn tại,
     * rồi cắt về đúng {@code limit}.
     */
    private List<BookResponseDTO> collectInOrder(List<Long> orderedBookIds, Map<Long, Book> booksById, int limit) {
        List<BookResponseDTO> result = new ArrayList<>();
        for (Long bookId : orderedBookIds) {
            Book book = booksById.get(bookId);
            if (book != null) {
                result.add(bookMapper.toDTO(book));
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        return result;
    }

    private Map<Long, Book> fetchActiveBooksById(List<Long> bookIds) {
        return bookRepository.findAllActiveByIdInWithCategory(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, Function.identity(), (a, b) -> a));
    }

    private List<BookResponseDTO> getCategoryFallback(Long bookId, int limit) {
        return bookRepository.findByIdWithCategory(bookId)
                .filter(book -> book.getCategory() != null)
                .map(book -> {
                    List<Book> sameCategory = bookRepository.findByCategoryExcludingWithCategory(
                            book.getCategory().getId(), bookId, PageRequest.of(0, limit));
                    return bookMapper.toDTOList(sameCategory);
                })
                .orElseGet(List::of);
    }

    private int clampLimit(int limit) {
        int fallback = recSysProperties.getDefaultRecommendationLimit();
        int effective = limit <= 0 ? fallback : limit;
        return Math.min(effective, recSysProperties.getMaxRecommendationLimit());
    }
}
