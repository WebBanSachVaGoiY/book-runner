package com.example.bookrunner.service.impl;

import com.example.bookrunner.dto.response.BestSellerStatDTO;
import com.example.bookrunner.dto.response.DashboardStatsResponseDTO;
import com.example.bookrunner.dto.response.RevenueChartDTO;
import com.example.bookrunner.repository.BookRepository;
import com.example.bookrunner.repository.OrderRepository;
import com.example.bookrunner.repository.UserRepository;
import com.example.bookrunner.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponseDTO getDashboardStats() {
        BigDecimal totalRevenue = orderRepository.sumTotalRevenue();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        long totalOrders = orderRepository.count();
        long totalUsers = userRepository.count();
        long totalBooks = bookRepository.countByActiveTrue();

        return DashboardStatsResponseDTO.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .totalUsers(totalUsers)
                .totalBooks(totalBooks)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevenueChartDTO> getMonthlyRevenue(Integer year) {
        int targetYear = (year != null && year > 0) ? year : LocalDate.now().getYear();

        Map<Integer, BigDecimal> monthlyRevenueMap = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            monthlyRevenueMap.put(m, BigDecimal.ZERO);
        }

        List<Object[]> queryResults = orderRepository.getMonthlyRevenueByYear(targetYear);
        if (queryResults != null) {
            for (Object[] row : queryResults) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    try {
                        int month = ((Number) row[0]).intValue();
                        BigDecimal revenue = new BigDecimal(row[1].toString());
                        if (month >= 1 && month <= 12) {
                            monthlyRevenueMap.put(month, revenue);
                        }
                    } catch (Exception ex) {
                        log.warn("Lỗi chuyển đổi dữ liệu doanh thu tháng: row[0]={}, row[1]={}", row[0], row[1], ex);
                    }
                }
            }
        }

        List<RevenueChartDTO> chartData = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            chartData.add(RevenueChartDTO.builder()
                    .month("T" + m)
                    .revenue(monthlyRevenueMap.get(m))
                    .build());
        }

        return chartData;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BestSellerStatDTO> getBestSellers(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        Pageable pageable = PageRequest.of(0, safeLimit);
        return bookRepository.findTopBestSellers(pageable);
    }
}
