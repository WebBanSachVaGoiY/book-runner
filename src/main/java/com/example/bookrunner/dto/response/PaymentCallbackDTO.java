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
public class PaymentCallbackDTO {
    private boolean success;
    private Long orderId;
    private String orderCode;
    private String transactionNo;
    private BigDecimal amount;
    private String bankCode;
    private String responseCode;
    private String message;
}
