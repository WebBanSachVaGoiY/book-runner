package com.example.bookrunner.service;

import com.example.bookrunner.dto.response.PaymentCallbackDTO;
import com.example.bookrunner.dto.response.VNPayPaymentResponseDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface VNPayService {

    /**
     * Tạo URL thanh toán VNPay Sandbox cho đơn hàng.
     *
     * @param userId  ID người dùng hiện tại (kiểm tra quyền sở hữu đơn)
     * @param orderId ID đơn hàng cần thanh toán
     * @param request HttpServletRequest để lấy địa chỉ IP của client
     * @return DTO chứa đường dẫn thanh toán và thông tin đơn hàng
     */
    VNPayPaymentResponseDTO createPaymentUrl(Long userId, Long orderId, HttpServletRequest request);

    /**
     * Xác thực kết quả giao dịch khi VNPay chuyển hướng người dùng về frontend (Return URL).
     *
     * @param queryParams Danh sách tham số query trả về từ VNPay
     * @return DTO chứa kết quả xác thực và trạng thái giao dịch
     */
    PaymentCallbackDTO processCallback(Map<String, String> queryParams);

    /**
     * Xử lý Webhook IPN chạy ngầm từ server VNPay gửi sang.
     *
     * @param queryParams Danh sách tham số gửi từ IPN
     * @return Map chứa RspCode và Message theo chuẩn quy định của VNPay
     */
    Map<String, String> processIpn(Map<String, String> queryParams);
}
