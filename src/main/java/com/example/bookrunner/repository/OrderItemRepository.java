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
}
