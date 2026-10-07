package com.example.bookrunner.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class BestSellerStatDTO {
    private String title;
    private Integer sold;

    public BestSellerStatDTO(String title, Number sold) {
        this.title = title;
        this.sold = (sold != null) ? sold.intValue() : 0;
    }
}
