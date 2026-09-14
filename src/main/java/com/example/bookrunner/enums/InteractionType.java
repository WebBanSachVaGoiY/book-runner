package com.example.bookrunner.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InteractionType {
    VIEW(1.0f),            // Click xem chi tiết sách
    ADD_TO_CART(3.0f),     // Thêm vào giỏ
    BUY(5.0f),             // Đã thanh toán đơn hàng
    RATING(0.0f);          // Đánh giá sao (weight sẽ lấy theo số sao)

    private final float defaultWeight;
}