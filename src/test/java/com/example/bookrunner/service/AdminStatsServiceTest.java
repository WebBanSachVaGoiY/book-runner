package com.example.bookrunner.service;

import com.example.bookrunner.dto.response.BestSellerStatDTO;
import com.example.bookrunner.dto.response.DashboardStatsResponseDTO;
import com.example.bookrunner.dto.response.RevenueChartDTO;
import com.example.bookrunner.repository.BookRepository;
import com.example.bookrunner.repository.OrderRepository;
import com.example.bookrunner.repository.UserRepository;
import com.example.bookrunner.service.impl.AdminStatsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private AdminStatsServiceImpl adminStatsService;

    @Test
    @DisplayName("getDashboardStats - Tính tổng doanh thu, tổng đơn hàng, người dùng và đầu sách")
    void getDashboardStats_Success() {
        when(orderRepository.sumTotalRevenue()).thenReturn(new BigDecimal("15000000"));
        when(orderRepository.count()).thenReturn(120L);
        when(userRepository.count()).thenReturn(50L);
        when(bookRepository.countByActiveTrue()).thenReturn(35L);

        DashboardStatsResponseDTO stats = adminStatsService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(new BigDecimal("15000000"), stats.getTotalRevenue());
        assertEquals(120L, stats.getTotalOrders());
        assertEquals(50L, stats.getTotalUsers());
        assertEquals(35L, stats.getTotalBooks());
    }

    @Test
    @DisplayName("getDashboardStats - Doanh thu null trả về BigDecimal.ZERO")
    void getDashboardStats_NullRevenue_ReturnsZero() {
        when(orderRepository.sumTotalRevenue()).thenReturn(null);
        when(orderRepository.count()).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(bookRepository.countByActiveTrue()).thenReturn(0L);

        DashboardStatsResponseDTO stats = adminStatsService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(BigDecimal.ZERO, stats.getTotalRevenue());
    }

    @Test
    @DisplayName("getMonthlyRevenue - Điền đủ 12 tháng từ T1 đến T12")
    void getMonthlyRevenue_Returns12Months() {
        List<Object[]> rawData = new ArrayList<>();
        rawData.add(new Object[]{1, new BigDecimal("2000000")});
        rawData.add(new Object[]{5, new BigDecimal("4500000")});

        when(orderRepository.getMonthlyRevenueByYear(2026)).thenReturn(rawData);

        List<RevenueChartDTO> result = adminStatsService.getMonthlyRevenue(2026);

        assertEquals(12, result.size());
        assertEquals("T1", result.get(0).getMonth());
        assertEquals(new BigDecimal("2000000"), result.get(0).getRevenue());

        assertEquals("T2", result.get(1).getMonth());
        assertEquals(BigDecimal.ZERO, result.get(1).getRevenue());

        assertEquals("T5", result.get(4).getMonth());
        assertEquals(new BigDecimal("4500000"), result.get(4).getRevenue());

        assertEquals("T12", result.get(11).getMonth());
        assertEquals(BigDecimal.ZERO, result.get(11).getRevenue());
    }

    @Test
    @DisplayName("getBestSellers - Giới hạn limit an toàn và trả danh sách")
    void getBestSellers_Success() {
        List<BestSellerStatDTO> mockBestSellers = List.of(
                new BestSellerStatDTO("Đắc Nhân Tâm", 150),
                new BestSellerStatDTO("Nhà Giả Kim", 120)
        );

        when(bookRepository.findTopBestSellers(any(Pageable.class))).thenReturn(mockBestSellers);

        List<BestSellerStatDTO> result = adminStatsService.getBestSellers(5);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Đắc Nhân Tâm", result.get(0).getTitle());
        assertEquals(150, result.get(0).getSold());
    }
}
