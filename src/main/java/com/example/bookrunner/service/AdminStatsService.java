package com.example.bookrunner.service;

import com.example.bookrunner.dto.response.BestSellerStatDTO;
import com.example.bookrunner.dto.response.DashboardStatsResponseDTO;
import com.example.bookrunner.dto.response.RevenueChartDTO;

import java.util.List;

public interface AdminStatsService {

    DashboardStatsResponseDTO getDashboardStats();

    List<RevenueChartDTO> getMonthlyRevenue(Integer year);

    List<BestSellerStatDTO> getBestSellers(int limit);
}
