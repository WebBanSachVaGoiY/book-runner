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
public class CategoryRevenueDTO {

    private Long categoryId;
    private String categoryName;
    private BigDecimal revenue;
    private Long quantitySold;
    private Double percentage;
}
