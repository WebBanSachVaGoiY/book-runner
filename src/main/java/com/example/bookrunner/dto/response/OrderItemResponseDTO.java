package com.example.bookrunner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDTO {

    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String coverImageUrl;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
}
