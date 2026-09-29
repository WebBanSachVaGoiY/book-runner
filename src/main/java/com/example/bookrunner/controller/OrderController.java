package com.example.bookrunner.controller;

import com.example.bookrunner.dto.common.ApiResponse;
import com.example.bookrunner.dto.request.CreateOrderRequest;
import com.example.bookrunner.dto.response.OrderResponseDTO;
import com.example.bookrunner.enums.OrderStatus;
import com.example.bookrunner.security.CustomUserDetails;
import com.example.bookrunner.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponseDTO>> createOrder(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderResponseDTO order = orderService.createOrder(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đặt hàng thành công", order));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<Page<OrderResponseDTO>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<OrderResponseDTO> orders = orderService.getMyOrders(currentUser.getId(), status, page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderDetail(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id
    ) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        OrderResponseDTO order = orderService.getOrderDetail(currentUser.getId(), id, isAdmin);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> cancelOrder(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id
    ) {
        OrderResponseDTO order = orderService.cancelOrder(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công", order));
    }
}
