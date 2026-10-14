package com.example.bookrunner.controller;

import com.example.bookrunner.dto.common.ApiResponse;
import com.example.bookrunner.dto.response.PaymentCallbackDTO;
import com.example.bookrunner.dto.response.VNPayPaymentResponseDTO;
import com.example.bookrunner.security.CustomUserDetails;
import com.example.bookrunner.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/vnpay")
@RequiredArgsConstructor
public class PaymentController {

    private final VNPayService vnPayService;

    @PostMapping("/create-url")
    public ResponseEntity<ApiResponse<VNPayPaymentResponseDTO>> createPaymentUrl(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam("orderId") Long orderId,
            HttpServletRequest request
    ) {
        Long userId = currentUser != null ? currentUser.getId() : null;
        VNPayPaymentResponseDTO response = vnPayService.createPaymentUrl(userId, orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Tạo liên kết thanh toán VNPay thành công", response));
    }

    @GetMapping("/callback-verify")
    public ResponseEntity<ApiResponse<PaymentCallbackDTO>> callbackVerify(
            @RequestParam Map<String, String> queryParams
    ) {
        PaymentCallbackDTO result = vnPayService.processCallback(queryParams);
        String message = result.isSuccess() ? "Thanh toán thành công" : result.getMessage();
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> processIpn(
            @RequestParam Map<String, String> queryParams
    ) {
        Map<String, String> response = vnPayService.processIpn(queryParams);
        return ResponseEntity.ok(response);
    }
}
