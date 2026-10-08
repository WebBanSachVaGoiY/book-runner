package com.example.bookrunner.dto.response;

import com.example.bookrunner.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusCountDTO {

    private OrderStatus status;
    private Long count;
    private Double percentage;
}
