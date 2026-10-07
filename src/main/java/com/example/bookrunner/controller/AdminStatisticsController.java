package com.example.bookrunner.controller;

import com.example.bookrunner.dto.common.ApiResponse;
import com.example.bookrunner.dto.response.RevenueStatisticsDTO;
import com.example.bookrunner.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {

    private final StatisticsService statisticsService;

    /**
     * Thống kê doanh thu theo khoảng thời gian.
     *
     * @param from ngày bắt đầu (mặc định 30 ngày trước)
     * @param to   ngày kết thúc (mặc định hôm nay)
     * @param topN số sản phẩm bán chạy cần lấy (mặc định 10)
     */
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueStatisticsDTO>> getRevenueStatistics(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "topN", defaultValue = "10") int topN) {
        // Xử lý giá trị mặc định
        if (to == null) {
            to = LocalDate.now();
        }
        if (from == null) {
            from = to.minusDays(30);
        }

        RevenueStatisticsDTO statistics = statisticsService.getRevenueStatistics(from, to, topN);
        return ResponseEntity.ok(ApiResponse.success("Thống kê doanh thu thành công", statistics));
    }
}
