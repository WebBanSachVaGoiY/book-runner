package com.example.bookrunner.dto.request;

import com.example.bookrunner.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "Họ tên người nhận không được để trống")
    private String recipientName;

    @NotBlank(message = "Số điện thoại nhận hàng không được để trống")
    private String recipientPhone;

    @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
    private String shippingAddress;

    private String note;

    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    /**
     * Danh sách sản phẩm muốn mua.
     * Nếu để trống (null hoặc empty), hệ thống sẽ tự động lấy toàn bộ sản phẩm trong giỏ hàng (Cart) của người dùng.
     */
    @Valid
    private List<OrderItemRequest> items;
}
