package com.example.bookrunner.dto.request;

import lombok.Data;

@Data
public class AddToCartRequest {
    private Long bookId;
    private Integer quantity;
}
