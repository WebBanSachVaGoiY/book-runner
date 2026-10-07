package com.example.bookrunner.repository;

import com.example.bookrunner.enums.OrderStatus;
import com.example.bookrunner.model.Order;
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
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(String orderCode);

    Boolean existsByOrderCode(String orderCode);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, OrderStatus status, Pageable pageable);

    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    long countByUserId(Long userId);

    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.items oi WHERE o.user.id = :userId AND oi.book.id = :bookId AND o.status = com.example.bookrunner.enums.OrderStatus.DELIVERED")
    boolean hasUserPurchasedBookAndDelivered(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o WHERE o.status = com.example.bookrunner.enums.OrderStatus.DELIVERED")
    BigDecimal sumTotalRevenue();

    @Query("SELECT MONTH(o.createdAt), COALESCE(SUM(o.finalAmount), 0) FROM Order o " +
            "WHERE YEAR(o.createdAt) = :year AND o.status = com.example.bookrunner.enums.OrderStatus.DELIVERED " +
            "GROUP BY MONTH(o.createdAt)")
    List<Object[]> getMonthlyRevenueByYear(@Param("year") int year);
}
