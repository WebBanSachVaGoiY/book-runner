package com.example.bookrunner.repository;

import com.example.bookrunner.model.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT oi2.book.id FROM OrderItem oi1 " +
            "JOIN OrderItem oi2 ON oi1.order.id = oi2.order.id " +
            "WHERE oi1.book.id = :bookId AND oi2.book.id != :bookId " +
            "GROUP BY oi2.book.id " +
            "ORDER BY COUNT(oi2.id) DESC")
    List<Long> findFrequentlyBoughtTogetherBookIds(@Param("bookId") Long bookId, Pageable pageable);

    @Query("SELECT oi.book.id FROM OrderItem oi " +
            "JOIN oi.order o " +
            "WHERE o.user.id = :userId " +
            "ORDER BY o.createdAt DESC")
    List<Long> findRecentPurchasedBookIdsByUserId(@Param("userId") Long userId, Pageable pageable);

    // ===== Thống kê doanh thu =====

    // Top sản phẩm bán chạy (theo đơn DELIVERED)
    @Query("SELECT oi.book.id, oi.book.title, oi.book.author, oi.book.coverImageUrl, " +
            "SUM(oi.quantity), SUM(oi.price * oi.quantity) " +
            "FROM OrderItem oi " +
            "JOIN oi.order o " +
            "WHERE o.status = 'DELIVERED' " +
            "AND o.createdAt >= :from AND o.createdAt <= :to " +
            "GROUP BY oi.book.id, oi.book.title, oi.book.author, oi.book.coverImageUrl " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findTopSellingProductsBetween(@Param("from") java.time.LocalDateTime from,
                                                 @Param("to") java.time.LocalDateTime to,
                                                 Pageable pageable);

    // Doanh thu theo danh mục (theo đơn DELIVERED)
    @Query("SELECT oi.book.category.id, oi.book.category.name, " +
            "SUM(oi.price * oi.quantity), SUM(oi.quantity) " +
            "FROM OrderItem oi " +
            "JOIN oi.order o " +
            "WHERE o.status = 'DELIVERED' " +
            "AND o.createdAt >= :from AND o.createdAt <= :to " +
            "GROUP BY oi.book.category.id, oi.book.category.name " +
            "ORDER BY SUM(oi.price * oi.quantity) DESC")
    List<Object[]> findCategoryRevenueBetween(@Param("from") java.time.LocalDateTime from,
                                              @Param("to") java.time.LocalDateTime to);

    // Tổng số sản phẩm đã bán (theo đơn DELIVERED)
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
            "JOIN oi.order o " +
            "WHERE o.status = 'DELIVERED' " +
            "AND o.createdAt >= :from AND o.createdAt <= :to")
    Long sumTotalProductsSoldBetween(@Param("from") java.time.LocalDateTime from,
                                     @Param("to") java.time.LocalDateTime to);
}
