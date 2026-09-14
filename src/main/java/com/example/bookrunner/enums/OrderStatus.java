package com.example.bookrunner.enums;

public enum OrderStatus {
    PENDING,        // Đang chờ xử lý
    CONFIRMED,      // Đã xác nhận
    SHIPPING,       // Đang giao hàng
    DELIVERED,      // Đã giao hàng thành công
    CANCELLED,      // Đã hủy đơn
    RETURNED        // Đã trả hàng / hoàn tiền
}
