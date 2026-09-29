package com.example.bookrunner.dto.response;

import com.example.bookrunner.enums.OrderStatus;
import com.example.bookrunner.enums.PaymentMethod;
import com.example.bookrunner.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {

    private Long id;
    private String orderCode;
    private Long userId;
    private String username;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private String note;
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private BigDecimal finalAmount;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private List<OrderItemResponseDTO> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
