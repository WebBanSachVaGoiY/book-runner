package com.example.bookrunner.service.impl;

import com.example.bookrunner.dto.request.BookRequestDTO;
import com.example.bookrunner.dto.response.BookResponseDTO;
import com.example.bookrunner.exception.BadRequestException;
import com.example.bookrunner.exception.DuplicateUniqueFieldException;
import com.example.bookrunner.exception.ItemNotFoundException;
import com.example.bookrunner.model.Book;
import com.example.bookrunner.model.Category;
import com.example.bookrunner.repository.BookRepository;
import com.example.bookrunner.repository.CategoryRepository;
import com.example.bookrunner.service.BookService;
import com.example.bookrunner.service.mapper.BookMapper;
import com.example.bookrunner.config.CacheNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final ModelMapper mapper;
    private final BookMapper bookMapper;
    private final CacheManager cacheManager;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "price", "averageRating", "totalReviews", "createdAt", "soldCount");

    // Lấy tất cả các sách (có lọc + phân trang)
    @Override
    @Transactional(readOnly = true)
    public Page<BookResponseDTO> findAll(String keyword, Long categoryId,
            BigDecimal minPrice, BigDecimal maxPrice, Boolean isFeatured,
            int page, int size,
            String sortBy, String sortDir) {
        // 1. Kiểm tra và chuẩn hóa sortBy và sortDir chống lỗi 500 do sai property
        String validSortBy = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) ? sortBy : "createdAt";
        boolean isAsc = "asc".equalsIgnoreCase(sortDir);
        Sort sort = isAsc ? Sort.by(validSortBy).ascending() : Sort.by(validSortBy).descending();

        // 2. Tạo Pageable (page 0-indexed trong Spring Data)
        Pageable pageable = PageRequest.of(page, size, sort);

        // 3. Gọi repository query kết hợp tìm kiếm + lọc (phân trang tại DB với LEFT
        // JOIN FETCH)
        Page<Book> bookPage = bookRepository.searchAndFilterBooks(
                keyword, categoryId, minPrice, maxPrice, isFeatured, pageable);

        // 4. Chuyển đổi Page<Book> → Page<BookResponseDTO> an toàn, zero entity
        // exposure
        return bookPage.map(this::mapToDTO);
    }

    // Lấy tất cả các sách cho Admin (không lọc active=true)
    @Override
    @Transactional(readOnly = true)
    public Page<BookResponseDTO> findAllForAdmin(String keyword, Long categoryId,
            BigDecimal minPrice, BigDecimal maxPrice,
            int page, int size,
            String sortBy, String sortDir) {
        String validSortBy = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) ? sortBy : "createdAt";
        boolean isAsc = "asc".equalsIgnoreCase(sortDir);
        Sort sort = isAsc ? Sort.by(validSortBy).ascending() : Sort.by(validSortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Book> bookPage = bookRepository.searchAndFilterBooksForAdmin(
                keyword, categoryId, minPrice, maxPrice, pageable);

        return bookPage.map(this::mapToDTO);
    }

    // Lấy chi tiết sách theo id (kèm FETCH category)
    @Override
    @Transactional(readOnly = true)
    public BookResponseDTO findById(Long id) {
        Book book = bookRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy sách với id: " + id));
        return mapToDTO(book);
    }

    private BookResponseDTO mapToDTO(Book book) {
        return bookMapper.toDTO(book);
    }

    // Thêm sách
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBook(BookRequestDTO bookRequestDTO) {
        Book newBook = mapper.map(bookRequestDTO, Book.class);
        newBook.setId(null);

        // Set default values cho các cột NOT NULL (ModelMapper không áp dụng
        // @Builder.Default)
        if (newBook.getStockQuantity() == null)
            newBook.setStockQuantity(0);
        if (newBook.getActive() == null)
            newBook.setActive(true);
        if (newBook.getIsFeatured() == null)
            newBook.setIsFeatured(false);
        if (newBook.getSoldCount() == null)
            newBook.setSoldCount(0);
        if (newBook.getAverageRating() == null)
            newBook.setAverageRating(0.0);
        if (newBook.getTotalReviews() == null)
            newBook.setTotalReviews(0);
        if (newBook.getLanguage() == null)
            newBook.setLanguage("Tiếng Việt");

        if (newBook.getIsbn() != null && !newBook.getIsbn().isBlank()) {
            String cleanIsbn = newBook.getIsbn().trim();
            if (bookRepository.findByIsbn(cleanIsbn).isPresent()) {
                throw new DuplicateUniqueFieldException(
                        "Mã định danh (ISBN) '" + cleanIsbn + "' đã tồn tại trong hệ thống!");
            }
            newBook.setIsbn(cleanIsbn);
        }

        // Lookup Category từ DB thay vì dùng transient object
        if (bookRequestDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(bookRequestDTO.getCategoryId())
                    .orElseThrow(() -> new ItemNotFoundException(
                            "Category không tồn tại với id: " + bookRequestDTO.getCategoryId()));
            newBook.setCategory(category);
        }

        bookRepository.save(newBook);
    }

    // Cập nhật thông tin sách
    @Override
    @Transactional
    public void updateBook(Long id, BookRequestDTO bookRequestDTO) {
        Book existingBook = bookRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Sách không tồn tại!"));

        // Kiểm tra ISBN trùng khi đổi ISBN
        if (bookRequestDTO.getIsbn() != null && !bookRequestDTO.getIsbn().equals(existingBook.getIsbn())) {
            bookRepository.findByIsbn(bookRequestDTO.getIsbn()).ifPresent(book -> {
                if (!book.getId().equals(id)) {
                    throw new DuplicateUniqueFieldException("Mã định danh ISBN bị trùng!");
                }
            });
        }

        // Chỉ cập nhật các field không null, giữ nguyên giá trị cũ cho field null
        if (bookRequestDTO.getTitle() != null)
            existingBook.setTitle(bookRequestDTO.getTitle());
        if (bookRequestDTO.getAuthor() != null)
            existingBook.setAuthor(bookRequestDTO.getAuthor());
        if (bookRequestDTO.getPublisher() != null)
            existingBook.setPublisher(bookRequestDTO.getPublisher());
        if (bookRequestDTO.getPublicationYear() != null)
            existingBook.setPublicationYear(bookRequestDTO.getPublicationYear());
        if (bookRequestDTO.getIsbn() != null)
            existingBook.setIsbn(bookRequestDTO.getIsbn());
        if (bookRequestDTO.getDescription() != null)
            existingBook.setDescription(bookRequestDTO.getDescription());
        if (bookRequestDTO.getPrice() != null)
            existingBook.setPrice(bookRequestDTO.getPrice());
        if (bookRequestDTO.getDiscountPrice() != null)
            existingBook.setDiscountPrice(bookRequestDTO.getDiscountPrice());
        if (bookRequestDTO.getStockQuantity() != null)
            existingBook.setStockQuantity(bookRequestDTO.getStockQuantity());
        if (bookRequestDTO.getCoverImageUrl() != null)
            existingBook.setCoverImageUrl(bookRequestDTO.getCoverImageUrl());
        if (bookRequestDTO.getPageCount() != null)
            existingBook.setPageCount(bookRequestDTO.getPageCount());
        if (bookRequestDTO.getLanguage() != null)
            existingBook.setLanguage(bookRequestDTO.getLanguage());
        if (bookRequestDTO.getIsFeatured() != null)
            existingBook.setIsFeatured(bookRequestDTO.getIsFeatured());
        if (bookRequestDTO.getActive() != null)
            existingBook.setActive(bookRequestDTO.getActive());
        if (bookRequestDTO.getSoldCount() != null)
            existingBook.setSoldCount(bookRequestDTO.getSoldCount());

        // Cập nhật category
        if (bookRequestDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(bookRequestDTO.getCategoryId())
                    .orElseThrow(() -> new ItemNotFoundException(
                            "Category không tồn tại với id: " + bookRequestDTO.getCategoryId()));
            existingBook.setCategory(category);
        }

        bookRepository.save(existingBook);
        evictBookRelatedCaches(id);
        evictTrendingCache();
    }

    // Xóa mềm (soft-delete) sách theo danh sách id
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBook(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("Danh sách ID sách cần xóa không được để trống!");
        }
        bookRepository.updateActiveStatusByIdIn(ids, false);
        for (Long id : ids) {
            evictBookRelatedCaches(id);
        }
        evictTrendingCache();
    }

    // Khôi phục sách đã xóa mềm
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreBook(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("Danh sách ID sách cần khôi phục không được để trống!");
        }
        bookRepository.updateActiveStatusByIdIn(ids, true);
        for (Long id : ids) {
            evictBookRelatedCaches(id);
        }
        evictTrendingCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BookResponseDTO toggleFeatured(Long id) {
        Book book = bookRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy sách với id: " + id));
        book.setIsFeatured(book.getIsFeatured() == null || !book.getIsFeatured());
        Book saved = bookRepository.save(book);
        evictBookRelatedCaches(id);
        evictTrendingCache();
        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookResponseDTO> findBestSellers() {
        Pageable pageable = PageRequest.of(0, 16, Sort.unsorted());
        Page<Book> bookPage = bookRepository.findBestSellers(pageable);
        return bookPage.map(this::mapToDTO);
    }

    private void evictBookRelatedCaches(Long bookId) {
        if (cacheManager == null || bookId == null) {
            return;
        }
        try {
            Cache relatedCache = cacheManager.getCache(CacheNames.RELATED);
            Cache fbtCache = cacheManager.getCache(CacheNames.FBT);
            for (int limit : List.of(4, 6, 8, 10, 12, 16, 20)) {
                if (relatedCache != null) {
                    relatedCache.evict(bookId + ":" + limit);
                }
                if (fbtCache != null) {
                    fbtCache.evict(bookId + ":" + limit);
                }
            }
        } catch (Exception e) {
            log.warn("Không thể xóa cache RELATED/FBT cho book {}: {}", bookId, e.getMessage());
        }
    }

    private void evictTrendingCache() {
        if (cacheManager == null) {
            return;
        }
        try {
            Cache trendingCache = cacheManager.getCache(CacheNames.TRENDING);
            if (trendingCache != null) {
                trendingCache.clear();
            }
        } catch (Exception e) {
            log.warn("Không thể xóa cache TRENDING: {}", e.getMessage());
        }
    }
}
