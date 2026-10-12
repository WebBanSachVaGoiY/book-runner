package com.example.bookrunner.service;

import com.example.bookrunner.config.VNPayConfig;
import com.example.bookrunner.dto.response.PaymentCallbackDTO;
import com.example.bookrunner.dto.response.VNPayPaymentResponseDTO;
import com.example.bookrunner.enums.OrderStatus;
import com.example.bookrunner.enums.PaymentMethod;
import com.example.bookrunner.enums.PaymentStatus;
import com.example.bookrunner.exception.BadRequestException;
import com.example.bookrunner.exception.ItemNotFoundException;
import com.example.bookrunner.model.Order;
import com.example.bookrunner.model.User;
import com.example.bookrunner.repository.BookRepository;
import com.example.bookrunner.repository.CartRepository;
import com.example.bookrunner.repository.OrderRepository;
import com.example.bookrunner.repository.UserBookInteractionRepository;
import com.example.bookrunner.service.impl.VNPayServiceImpl;
import com.example.bookrunner.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VNPayServiceTest {

    @Mock
    private VNPayConfig vnPayConfig;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserBookInteractionRepository interactionRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private VNPayServiceImpl vnPayService;

    private Order testOrder;
    private User testUser;
    private final String HASH_SECRET = "XNBCJFAKAZQSGZSSTGZQVOENXWIGWLGR";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("customer01")
                .build();

        testOrder = Order.builder()
                .id(10L)
                .orderCode("ORD-1728472912345")
                .user(testUser)
                .finalAmount(new BigDecimal("250000"))
                .status(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
    }

    private void mockVNPayConfig() {
        when(vnPayConfig.getVersion()).thenReturn("2.1.0");
        when(vnPayConfig.getCommand()).thenReturn("pay");
        when(vnPayConfig.getTmnCode()).thenReturn("CGXZLS0Z");
        when(vnPayConfig.getUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        when(vnPayConfig.getReturnUrl()).thenReturn("http://localhost:3000/payment/vnpay-return");
        when(vnPayConfig.getHashSecret()).thenReturn(HASH_SECRET);
    }

    @Test
    @DisplayName("createPaymentUrl - Sinh URL thanh toán VNPay thành công")
    void createPaymentUrl_Success() {
        mockVNPayConfig();
        when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));
        when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");

        VNPayPaymentResponseDTO result = vnPayService.createPaymentUrl(1L, 10L, request);

        assertNotNull(result);
        assertEquals(10L, result.getOrderId());
        assertEquals("ORD-1728472912345", result.getOrderCode());
        assertEquals(new BigDecimal("250000"), result.getAmount());
        assertNotNull(result.getPaymentUrl());
        assertTrue(result.getPaymentUrl().startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"));
        assertTrue(result.getPaymentUrl().contains("vnp_Amount=25000000"));
        assertTrue(result.getPaymentUrl().contains("vnp_TxnRef=ORD-1728472912345"));
        assertTrue(result.getPaymentUrl().contains("vnp_SecureHash="));
    }

    @Test
    @DisplayName("createPaymentUrl - Ném ItemNotFoundException khi không tìm thấy đơn")
    void createPaymentUrl_OrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () ->
                vnPayService.createPaymentUrl(1L, 99L, request)
        );
    }

    @Test
    @DisplayName("createPaymentUrl - Ném BadRequestException khi khác người dùng")
    void createPaymentUrl_WrongUser() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));

        assertThrows(BadRequestException.class, () ->
                vnPayService.createPaymentUrl(2L, 10L, request)
        );
    }

    @Test
    @DisplayName("createPaymentUrl - Ném BadRequestException khi đơn đã thanh toán")
    void createPaymentUrl_AlreadyPaid() {
        testOrder.setPaymentStatus(PaymentStatus.PAID);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(testOrder));

        assertThrows(BadRequestException.class, () ->
                vnPayService.createPaymentUrl(1L, 10L, request)
        );
    }

    @Test
    @DisplayName("processCallback - Xử lý Return Callback thành công cập nhật PAID và CONFIRMED")
    void processCallback_Success() {
        when(vnPayConfig.getHashSecret()).thenReturn(HASH_SECRET);
        when(orderRepository.findByOrderCode("ORD-1728472912345")).thenReturn(Optional.of(testOrder));

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "ORD-1728472912345");
        params.put("vnp_Amount", "25000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "14123456");
        params.put("vnp_BankCode", "NCB");

        // Tính hash chuẩn
        String[] hashAndQuery = VNPayUtil.buildHashAndQuery(params);
        String secureHash = VNPayUtil.hmacSHA512(HASH_SECRET, hashAndQuery[0]);
        params.put("vnp_SecureHash", secureHash);

        PaymentCallbackDTO callbackResult = vnPayService.processCallback(params);

        assertTrue(callbackResult.isSuccess());
        assertEquals(PaymentStatus.PAID, testOrder.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, testOrder.getStatus());
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    @DisplayName("processCallback - Chữ ký không hợp lệ trả về success=false")
    void processCallback_InvalidSignature() {
        when(vnPayConfig.getHashSecret()).thenReturn(HASH_SECRET);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "ORD-1728472912345");
        params.put("vnp_Amount", "25000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_SecureHash", "invalid_hash_value");

        PaymentCallbackDTO callbackResult = vnPayService.processCallback(params);

        assertFalse(callbackResult.isSuccess());
        assertTrue(callbackResult.getMessage().contains("không hợp lệ"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("processCallback - Khách hàng hủy hoặc thanh toán thất bại thì xóa đơn hàng và hoàn tồn kho")
    void processCallback_CancelledOrFailed_RollbacksStockAndDeletesOrder() {
        when(vnPayConfig.getHashSecret()).thenReturn(HASH_SECRET);
        when(orderRepository.findByOrderCode("ORD-1728472912345")).thenReturn(Optional.of(testOrder));

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "ORD-1728472912345");
        params.put("vnp_Amount", "25000000");
        params.put("vnp_ResponseCode", "24"); // Mã 24: Khách hàng hủy giao dịch

        String[] hashAndQuery = VNPayUtil.buildHashAndQuery(params);
        String secureHash = VNPayUtil.hmacSHA512(HASH_SECRET, hashAndQuery[0]);
        params.put("vnp_SecureHash", secureHash);

        PaymentCallbackDTO callbackResult = vnPayService.processCallback(params);

        assertFalse(callbackResult.isSuccess());
        assertNull(callbackResult.getOrderId());
        assertEquals("ORD-1728472912345", callbackResult.getOrderCode());
        assertEquals("24", callbackResult.getResponseCode());
        assertTrue(callbackResult.getMessage().contains("hủy giao dịch"));

        // Xác minh đơn hàng bị xóa khỏi DB
        verify(orderRepository, times(1)).delete(testOrder);
    }

    @Test
    @DisplayName("processIpn - Webhook IPN hợp lệ cập nhật PAID và trả về RspCode 00")
    void processIpn_Success() {
        when(vnPayConfig.getHashSecret()).thenReturn(HASH_SECRET);
        when(orderRepository.findByOrderCode("ORD-1728472912345")).thenReturn(Optional.of(testOrder));

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "ORD-1728472912345");
        params.put("vnp_Amount", "25000000");
        params.put("vnp_ResponseCode", "00");

        String[] hashAndQuery = VNPayUtil.buildHashAndQuery(params);
        String secureHash = VNPayUtil.hmacSHA512(HASH_SECRET, hashAndQuery[0]);
        params.put("vnp_SecureHash", secureHash);

        Map<String, String> ipnResult = vnPayService.processIpn(params);

        assertEquals("00", ipnResult.get("RspCode"));
        assertEquals("Confirm Success", ipnResult.get("Message"));
        assertEquals(PaymentStatus.PAID, testOrder.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, testOrder.getStatus());
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    @DisplayName("processIpn - Sai số tiền trả về RspCode 04")
    void processIpn_InvalidAmount() {
        when(vnPayConfig.getHashSecret()).thenReturn(HASH_SECRET);
        when(orderRepository.findByOrderCode("ORD-1728472912345")).thenReturn(Optional.of(testOrder));

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "ORD-1728472912345");
        params.put("vnp_Amount", "10000000"); // 100k thay vì 250k
        params.put("vnp_ResponseCode", "00");

        String[] hashAndQuery = VNPayUtil.buildHashAndQuery(params);
        String secureHash = VNPayUtil.hmacSHA512(HASH_SECRET, hashAndQuery[0]);
        params.put("vnp_SecureHash", secureHash);

        Map<String, String> ipnResult = vnPayService.processIpn(params);

        assertEquals("04", ipnResult.get("RspCode"));
        assertEquals("Invalid Amount", ipnResult.get("Message"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("processIpn - Sai chữ ký trả về RspCode 97")
    void processIpn_InvalidChecksum() {
        when(vnPayConfig.getHashSecret()).thenReturn(HASH_SECRET);

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "ORD-1728472912345");
        params.put("vnp_Amount", "25000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_SecureHash", "fake_hash");

        Map<String, String> ipnResult = vnPayService.processIpn(params);

        assertEquals("97", ipnResult.get("RspCode"));
        assertEquals("Invalid Checksum", ipnResult.get("Message"));
        verify(orderRepository, never()).save(any());
    }
}
