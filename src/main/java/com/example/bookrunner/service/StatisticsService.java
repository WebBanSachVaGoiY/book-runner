package com.example.bookrunner.service;

import com.example.bookrunner.dto.response.RevenueStatisticsDTO;

import java.time.LocalDate;

public interface StatisticsService {

    /**
     * Lấy thống kê doanh thu theo khoảng thời gian.
     *
     * @param from ngày bắt đầu (mặc định 30 ngày trước nếu null)
     * @param to   ngày kết thúc (mặc định hôm nay nếu null)
     * @param topN số lượng sản phẩm bán chạy muốn lấy (mặc định 10)
     * @return thống kê doanh thu tổng hợp
     */
    RevenueStatisticsDTO getRevenueStatistics(LocalDate from, LocalDate to, int topN);
}
