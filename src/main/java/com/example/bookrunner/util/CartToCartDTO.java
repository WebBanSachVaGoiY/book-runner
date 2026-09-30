package com.example.bookrunner.util;

import com.example.bookrunner.dto.CartDTO;
import com.example.bookrunner.dto.CartItemDTO;
import com.example.bookrunner.model.Cart;
import com.example.bookrunner.model.CartItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartToCartDTO {
    public static CartDTO toCartDTO(Cart cart){
        CartDTO result = new CartDTO();
        result.setUserId(cart.getUser().getId());
        List<CartItemDTO> itemDTOList = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        Integer totalQuantity = 0;
        for (CartItem i:cart.getItems()){
            CartItemDTO itemDTO = CartItemToDTO.toCartItemDTO(i);
            itemDTOList.add(itemDTO);
            totalQuantity += i.getQuantity();
            if (itemDTO.getUnitPrice() != null) {
                totalPrice = totalPrice.add(itemDTO.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
            }
        }
        result.setCartItemDTOList(itemDTOList);
        result.setTotalQuantity(totalQuantity);
        result.setTotalPrice(totalPrice);
        return result;
    }
}
