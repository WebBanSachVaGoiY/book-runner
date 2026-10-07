package com.example.bookrunner.service.impl;

import com.example.bookrunner.dto.response.*;
import com.example.bookrunner.enums.OrderStatus;
import com.example.bookrunner.repository.OrderItemRepository;
import com.example.bookrunner.repository.OrderRepository;
import com.example.bookrunner.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public RevenueStatisticsDTO getRevenueStatistics(LocalDate from, LocalDate to, int topN) {
        // Xác định khoảng thời gian
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(LocalTime.MAX);

        log.info("Thống kê doanh thu từ {} đến {}, top {}", from, to, topN);

        // 1. Tổng doanh thu (chỉ tính đơn DELIVERED)
        BigDecimal totalRevenue = orderRepository.sumRevenueByDeliveredBetween(fromDateTime, toDateTime);
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        // 2. Tổng số đơn hàng
        Long totalOrders = orderRepository.countOrdersBetween(fromDateTime, toDateTime);

        // 3. Đếm đơn DELIVERED và CANCELLED
        Long totalDeliveredOrders = orderRepository.countByStatusBetween(OrderStatus.DELIVERED, fromDateTime, toDateTime);
        Long totalCancelledOrders = orderRepository.countByStatusBetween(OrderStatus.CANCELLED, fromDateTime, toDateTime);

        // 4. Giá trị đơn hàng trung bình (doanh thu / số đơn delivered)
        BigDecimal averageOrderValue = BigDecimal.ZERO;
        if (totalDeliveredOrders != null && totalDeliveredOrders > 0) {
            averageOrderValue = totalRevenue.divide(BigDecimal.valueOf(totalDeliveredOrders), 2, RoundingMode.HALF_UP);
        }

        // 5. Tổng số sản phẩm đã bán
        Long totalProductsSold = orderItemRepository.sumTotalProductsSoldBetween(fromDateTime, toDateTime);
        if (totalProductsSold == null) {
            totalProductsSold = 0L;
        }

        // 6. Doanh thu theo ngày
        List<DailyRevenueDTO> dailyRevenues = buildDailyRevenues(fromDateTime, toDateTime);

        // 7. Phân bổ theo trạng thái
        List<OrderStatusCountDTO> orderStatusBreakdown = buildOrderStatusBreakdown(fromDateTime, toDateTime, totalOrders);

        // 8. Top sản phẩm bán chạy
        List<TopSellingProductDTO> topSellingProducts = buildTopSellingProducts(fromDateTime, toDateTime, topN);

        // 9. Doanh thu theo danh mục
        List<CategoryRevenueDTO> categoryRevenues = buildCategoryRevenues(fromDateTime, toDateTime, totalRevenue);

        return RevenueStatisticsDTO.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .totalDeliveredOrders(totalDeliveredOrders)
                .totalCancelledOrders(totalCancelledOrders)
                .averageOrderValue(averageOrderValue)
                .totalProductsSold(totalProductsSold)
                .dailyRevenues(dailyRevenues)
                .orderStatusBreakdown(orderStatusBreakdown)
                .topSellingProducts(topSellingProducts)
                .categoryRevenues(categoryRevenues)
                .build();
    }

    /**
     * Xây dựng danh sách doanh thu theo ngày.
     */
    private List<DailyRevenueDTO> buildDailyRevenues(LocalDateTime from, LocalDateTime to) {
        List<Object[]> results = orderRepository.findDailyRevenueBetween(from, to);
        return results.stream().map(row -> DailyRevenueDTO.builder()
                .date((LocalDate) row[0])
                .revenue((BigDecimal) row[1])
                .orderCount((Long) row[2])
                .build()
        ).collect(Collectors.toList());
    }

    /**
     * Xây dựng phân bổ số đơn theo trạng thái + tính phần trăm.
     */
    private List<OrderStatusCountDTO> buildOrderStatusBreakdown(LocalDateTime from, LocalDateTime to, Long totalOrders) {
        List<Object[]> results = orderRepository.countOrdersByStatusBetween(from, to);
        List<OrderStatusCountDTO> breakdown = new ArrayList<>();

        for (Object[] row : results) {
            OrderStatus status = (OrderStatus) row[0];
            Long count = (Long) row[1];
            double percentage = (totalOrders != null && totalOrders > 0)
                    ? (count * 100.0 / totalOrders)
                    : 0.0;
            // Làm tròn 1 chữ số thập phân
            percentage = Math.round(percentage * 10.0) / 10.0;

            breakdown.add(OrderStatusCountDTO.builder()
                    .status(status)
                    .count(count)
                    .percentage(percentage)
                    .build());
        }

        return breakdown;
    }

    /**
     * Xây dựng danh sách top sản phẩm bán chạy.
     */
    private List<TopSellingProductDTO> buildTopSellingProducts(LocalDateTime from, LocalDateTime to, int topN) {
        List<Object[]> results = orderItemRepository.findTopSellingProductsBetween(from, to, PageRequest.of(0, topN));
        return results.stream().map(row -> TopSellingProductDTO.builder()
                .bookId((Long) row[0])
                .title((String) row[1])
                .author((String) row[2])
                .coverImageUrl((String) row[3])
                .quantitySold((Long) row[4])
                .revenue((BigDecimal) row[5])
                .build()
        ).collect(Collectors.toList());
    }

    /**
     * Xây dựng danh sách doanh thu theo danh mục + tính phần trăm.
     */
    private List<CategoryRevenueDTO> buildCategoryRevenues(LocalDateTime from, LocalDateTime to, BigDecimal totalRevenue) {
        List<Object[]> results = orderItemRepository.findCategoryRevenueBetween(from, to);
        return results.stream().map(row -> {
            BigDecimal revenue = (BigDecimal) row[2];
            double percentage = (totalRevenue.compareTo(BigDecimal.ZERO) > 0)
                    ? revenue.multiply(BigDecimal.valueOf(100))
                    .divide(totalRevenue, 1, RoundingMode.HALF_UP)
                    .doubleValue()
                    : 0.0;

            return CategoryRevenueDTO.builder()
                    .categoryId((Long) row[0])
                    .categoryName((String) row[1])
                    .revenue(revenue)
                    .quantitySold((Long) row[3])
                    .percentage(percentage)
                    .build();
        }).collect(Collectors.toList());
    }
}
