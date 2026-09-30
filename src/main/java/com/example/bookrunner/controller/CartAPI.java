package com.example.bookrunner.controller;

import com.example.bookrunner.dto.CartDTO;
import com.example.bookrunner.dto.request.AddToCartRequest;
import com.example.bookrunner.security.CustomUserDetails;
import com.example.bookrunner.service.CartService;
import lombok.RequiredArgsConstructor;
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
    public CartDTO getCart(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return cartService.getCart(customUserDetails.getId());
    }

    @PostMapping("/items")
    public void addEditCart(@AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody AddToCartRequest addToCartRequest) {
        cartService.addEditCart(customUserDetails.getId(), addToCartRequest.getBookId(),
                addToCartRequest.getQuantity());
    }

    @DeleteMapping("/items")
    public ResponseEntity<Void> deleteCartItems(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody List<Long> bookIds) {
        cartService.deleteCartItem(customUserDetails.getId(), bookIds);
        return ResponseEntity.noContent().build();
    }
}
