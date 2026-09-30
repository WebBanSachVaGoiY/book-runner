package com.example.bookrunner.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemDTO {
    private Long cartId;
    private Long bookId;
    private String productName;
    private String imageUrl;
    private Integer quantity = 1;
    private BigDecimal originalPrice;
    private BigDecimal unitPrice;
    private Integer availableStock;
    private Boolean isActive;
}
