package com.example.bookrunner.repository;

import com.example.bookrunner.enums.OrderStatus;
import com.example.bookrunner.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    // ===== Thống kê doanh thu =====

    // Đếm số đơn theo trạng thái trong khoảng thời gian
    @Query("SELECT o.status, COUNT(o) FROM Order o " +
            "WHERE o.createdAt >= :from AND o.createdAt <= :to " +
            "GROUP BY o.status")
    List<Object[]> countOrdersByStatusBetween(@Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);

    // Tổng doanh thu (finalAmount) của các đơn DELIVERED trong khoảng thời gian
    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o " +
            "WHERE o.status = 'DELIVERED' " +
            "AND o.createdAt >= :from AND o.createdAt <= :to")
    java.math.BigDecimal sumRevenueByDeliveredBetween(@Param("from") LocalDateTime from,
                                                      @Param("to") LocalDateTime to);

    // Tổng số đơn hàng trong khoảng thời gian
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.createdAt >= :from AND o.createdAt <= :to")
    Long countOrdersBetween(@Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to);

    // Doanh thu theo ngày (nhóm theo DATE(createdAt))
    @Query("SELECT CAST(o.createdAt AS LocalDate), COALESCE(SUM(o.finalAmount), 0), COUNT(o) " +
            "FROM Order o " +
            "WHERE o.status = 'DELIVERED' " +
            "AND o.createdAt >= :from AND o.createdAt <= :to " +
            "GROUP BY CAST(o.createdAt AS LocalDate) " +
            "ORDER BY CAST(o.createdAt AS LocalDate) ASC")
    List<Object[]> findDailyRevenueBetween(@Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    // Đếm số đơn theo trạng thái cụ thể trong khoảng thời gian
    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.status = :status " +
            "AND o.createdAt >= :from AND o.createdAt <= :to")
    Long countByStatusBetween(@Param("status") OrderStatus status,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);
}
