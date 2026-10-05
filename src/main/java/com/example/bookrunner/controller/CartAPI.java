package com.example.bookrunner.controller;

import com.example.bookrunner.dto.CartDTO;
import com.example.bookrunner.dto.common.ApiResponse;
import com.example.bookrunner.dto.request.AddToCartRequest;
import com.example.bookrunner.dto.request.UpdateCartItemRequest;
import com.example.bookrunner.security.CustomUserDetails;
import com.example.bookrunner.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartAPI {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartDTO>> getCart(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        CartDTO cart = cartService.getCart(customUserDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin giỏ hàng thành công", cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<Void>> addToCart(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody AddToCartRequest addToCartRequest) {
        cartService.addToCart(customUserDetails.getId(), addToCartRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm vào giỏ hàng thành công", null));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartDTO>> updateCartItemQuantity(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        CartDTO updatedCart = cartService.updateCartItemQuantity(
                customUserDetails.getId(), itemId, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật số lượng sản phẩm thành công", updatedCart));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long itemId) {
        cartService.removeCartItem(customUserDetails.getId(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm khỏi giỏ hàng thành công", null));
    }

    @DeleteMapping("/items")
    public ResponseEntity<ApiResponse<Void>> deleteCartItems(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody List<Long> bookIds) {
        cartService.deleteCartItem(customUserDetails.getId(), bookIds);
        return ResponseEntity.ok(ApiResponse.success("Xóa danh sách sản phẩm khỏi giỏ hàng thành công", null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        cartService.clearCart(customUserDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Xóa sạch giỏ hàng thành công", null));
    }
}
