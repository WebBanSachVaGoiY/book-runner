package com.example.bookrunner.service;

import com.example.bookrunner.dto.request.CreateOrderRequest;
import com.example.bookrunner.dto.response.OrderResponseDTO;
import com.example.bookrunner.enums.OrderStatus;
import org.springframework.data.domain.Page;

public interface OrderService {

    OrderResponseDTO createOrder(Long userId, CreateOrderRequest request);

    Page<OrderResponseDTO> getMyOrders(Long userId, OrderStatus status, int page, int size);

    OrderResponseDTO getOrderDetail(Long userId, Long orderId, boolean isAdmin);

    OrderResponseDTO cancelOrder(Long userId, Long orderId);

    Page<OrderResponseDTO> getAllOrdersForAdmin(OrderStatus status, int page, int size);

    OrderResponseDTO updateOrderStatusForAdmin(Long orderId, OrderStatus newStatus);
}
