package com.example.bookrunner.repository;

import com.example.bookrunner.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    Page<Book> findByActiveTrue(Pageable pageable);

    Page<Book> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

    Page<Book> findByCategorySlugAndActiveTrue(String slug, Pageable pageable);

    Page<Book> findByIsFeaturedTrueAndActiveTrue(Pageable pageable);

    Page<Book> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Book> findByAuthorAndIdNotAndActiveTrue(String author, Long bookId, Pageable pageable);

    Page<Book> findByCategoryIdAndIdNotAndActiveTrue(Long categoryId, Long bookId, Pageable pageable);

    Page<Book> findByActiveTrueAndAverageRatingGreaterThanEqualOrderByAverageRatingDesc(
            Double minRating,
            Pageable pageable
    );

    @Query("SELECT b FROM Book b WHERE b.active = true AND " +
            "(LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.publisher) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Book> searchBooks(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.active = true " +
            "AND (:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(b.publisher) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:categoryId IS NULL OR b.category.id = :categoryId) " +
            "AND (:minPrice IS NULL OR b.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR b.price <= :maxPrice)")
    Page<Book> searchAndFilterBooks(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    @Query("SELECT b FROM Book b JOIN OrderItem oi ON b.id = oi.book.id " +
            "JOIN oi.order o WHERE b.active = true AND o.status != com.example.bookrunner.enums.OrderStatus.CANCELLED " +
            "GROUP BY b.id, b.title, b.author, b.publisher, b.publicationYear, b.isbn, b.description, " +
            "b.price, b.discountPrice, b.stockQuantity, b.coverImageUrl, b.pageCount, b.language, " +
            "b.averageRating, b.totalReviews, b.active, b.isFeatured, b.category, b.createdAt, b.updatedAt " +
            "ORDER BY SUM(oi.quantity) DESC")
    Page<Book> findBestSellers(Pageable pageable);

    List<Book> findAllByIdInAndActiveTrue(List<Long> ids);

    void deleteByIdIn(List<Long> ids);
}
