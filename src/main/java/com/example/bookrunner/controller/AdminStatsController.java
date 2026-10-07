package com.example.bookrunner.controller;

import com.example.bookrunner.dto.common.ApiResponse;
import com.example.bookrunner.dto.response.BestSellerStatDTO;
import com.example.bookrunner.dto.response.DashboardStatsResponseDTO;
import com.example.bookrunner.dto.response.RevenueChartDTO;
import com.example.bookrunner.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardStatsResponseDTO>> getStats() {
        DashboardStatsResponseDTO stats = adminStatsService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê tổng quan thành công", stats));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<List<RevenueChartDTO>>> getRevenueChart(
            @RequestParam(required = false) Integer year
    ) {
        List<RevenueChartDTO> revenueChart = adminStatsService.getMonthlyRevenue(year);
        return ResponseEntity.ok(ApiResponse.success("Lấy biểu đồ doanh thu thành công", revenueChart));
    }

    @GetMapping("/best-sellers")
    public ResponseEntity<ApiResponse<List<BestSellerStatDTO>>> getBestSellers(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<BestSellerStatDTO> bestSellers = adminStatsService.getBestSellers(limit);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sách bán chạy thành công", bestSellers));
    }
}
