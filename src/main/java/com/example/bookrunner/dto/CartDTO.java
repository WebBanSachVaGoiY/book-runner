package com.example.bookrunner.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartDTO {
    private Long userId;
    private List<CartItemDTO> cartItemDTOList;
    private Integer totalQuantity;
    private BigDecimal totalPrice;
}
