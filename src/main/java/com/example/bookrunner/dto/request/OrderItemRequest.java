package com.example.bookrunner.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {

    @NotNull(message = "Book ID không được để trống")
    private Long bookId;

    @NotNull(message = "Số lượng mua không được để trống")
    @Min(value = 1, message = "Số lượng mua tối thiểu là 1")
    private Integer quantity;
}
