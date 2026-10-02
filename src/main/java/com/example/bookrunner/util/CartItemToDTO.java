package com.example.bookrunner.util;

import com.example.bookrunner.dto.CartItemDTO;
import com.example.bookrunner.model.Book;
import com.example.bookrunner.model.Cart;
import com.example.bookrunner.model.CartItem;

import java.math.BigDecimal;

public class CartItemToDTO {
    public static CartItemDTO toCartItemDTO(CartItem cartItem){
        CartItemDTO result = new CartItemDTO();
        Cart cart = cartItem.getCart();
        Book book = cartItem.getBook();
        result.setCartId(cart.getId());
        result.setBookId(book.getId());
        result.setQuantity(cartItem.getQuantity());
        result.setProductName(book.getTitle());
        result.setImageUrl(book.getCoverImageUrl());
        result.setOriginalPrice(book.getPrice());
        BigDecimal actualPrice = (book.getDiscountPrice() != null && book.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0)
                ? book.getDiscountPrice()
                : book.getPrice();
        result.setUnitPrice(actualPrice);
        result.setAvailableStock(book.getStockQuantity());
        result.setIsActive(book.getActive());
        return result;
    }
}
