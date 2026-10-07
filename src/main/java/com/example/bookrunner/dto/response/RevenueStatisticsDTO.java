package com.example.bookrunner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStatisticsDTO {

    // Tổng quan
    private BigDecimal totalRevenue;             // Tổng doanh thu (đơn DELIVERED)
    private Long totalOrders;                    // Tổng số đơn hàng
    private Long totalDeliveredOrders;           // Số đơn giao thành công
    private Long totalCancelledOrders;           // Số đơn đã hủy
    private BigDecimal averageOrderValue;        // Giá trị đơn hàng trung bình
    private Long totalProductsSold;              // Tổng số sản phẩm đã bán

    // Doanh thu theo ngày
    private List<DailyRevenueDTO> dailyRevenues;

    // Phân bổ theo trạng thái đơn hàng
    private List<OrderStatusCountDTO> orderStatusBreakdown;

    // Top sản phẩm bán chạy
    private List<TopSellingProductDTO> topSellingProducts;

    // Doanh thu theo danh mục
    private List<CategoryRevenueDTO> categoryRevenues;
}
