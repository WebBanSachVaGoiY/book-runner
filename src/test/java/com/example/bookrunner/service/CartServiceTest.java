package com.example.bookrunner.service;

import com.example.bookrunner.dto.CartDTO;
import com.example.bookrunner.exception.BadRequestException;
import com.example.bookrunner.model.Book;
import com.example.bookrunner.model.Cart;
import com.example.bookrunner.model.CartItem;
import com.example.bookrunner.model.User;
import com.example.bookrunner.repository.BookRepository;
import com.example.bookrunner.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private CartService cartService;

    private User testUser;
    private Cart testCart;
    private Book bookWithoutDiscount;
    private Book bookWithDiscount;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        testCart = Cart.builder()
                .id(10L)
                .user(testUser)
                .items(new ArrayList<>())
                .build();

        bookWithoutDiscount = Book.builder()
                .id(100L)
                .title("Clean Code")
                .price(new BigDecimal("150000"))
                .discountPrice(null)
                .stockQuantity(10)
                .active(true)
                .build();

        bookWithDiscount = Book.builder()
                .id(200L)
                .title("Refactoring")
                .price(new BigDecimal("200000"))
                .discountPrice(new BigDecimal("180000"))
                .stockQuantity(5)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Lấy giỏ hàng thành công với sách không giảm giá (unitPrice fallback về price)")
    void getCart_bookWithoutDiscount_usesOriginalPrice() {
        CartItem item = CartItem.builder()
                .id(1L)
                .cart(testCart)
                .book(bookWithoutDiscount)
                .quantity(2)
                .build();
        testCart.getItems().add(item);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));

        CartDTO result = cartService.getCart(1L);

        assertNotNull(result);
        assertEquals(1, result.getCartItemDTOList().size());
        assertEquals(new BigDecimal("150000"), result.getCartItemDTOList().get(0).getUnitPrice());
        assertEquals(new BigDecimal("300000"), result.getTotalPrice());
        assertEquals(2, result.getTotalQuantity());
    }

    @Test
    @DisplayName("Lấy giỏ hàng thành công với sách có giảm giá (unitPrice dùng discountPrice)")
    void getCart_bookWithDiscount_usesDiscountPrice() {
        CartItem item = CartItem.builder()
                .id(2L)
                .cart(testCart)
                .book(bookWithDiscount)
                .quantity(3)
                .build();
        testCart.getItems().add(item);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));

        CartDTO result = cartService.getCart(1L);

        assertNotNull(result);
        assertEquals(1, result.getCartItemDTOList().size());
        assertEquals(new BigDecimal("180000"), result.getCartItemDTOList().get(0).getUnitPrice());
        assertEquals(new BigDecimal("540000"), result.getTotalPrice());
        assertEquals(3, result.getTotalQuantity());
    }

    @Test
    @DisplayName("Thêm sách mới vào giỏ hàng thành công")
    void addEditCart_newBook_success() {
        when(bookRepository.findById(100L)).thenReturn(Optional.of(bookWithoutDiscount));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        cartService.addEditCart(1L, 100L, 2);

        assertEquals(1, testCart.getItems().size());
        assertEquals(2, testCart.getItems().get(0).getQuantity());
        verify(cartRepository, times(1)).save(testCart);
    }

    @Test
    @DisplayName("Thêm sách vượt quá tồn kho ném BadRequestException")
    void addEditCart_exceedStock_throwsBadRequest() {
        when(bookRepository.findById(100L)).thenReturn(Optional.of(bookWithoutDiscount));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));

        assertThrows(BadRequestException.class, () -> cartService.addEditCart(1L, 100L, 15));
    }

    @Test
    @DisplayName("Xóa sản phẩm trong giỏ hàng thành công")
    void deleteCartItem_success() {
        CartItem item = CartItem.builder()
                .id(1L)
                .cart(testCart)
                .book(bookWithoutDiscount)
                .quantity(1)
                .build();
        testCart.getItems().add(item);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        cartService.deleteCartItem(1L, List.of(100L));

        assertTrue(testCart.getItems().isEmpty());
        verify(cartRepository, times(1)).save(testCart);
    }

    @Test
    @DisplayName("Cập nhật số lượng sản phẩm trong giỏ thành công")
    void updateCartItemQuantity_success() {
        CartItem item = CartItem.builder()
                .id(1L)
                .cart(testCart)
                .book(bookWithoutDiscount)
                .quantity(1)
                .build();
        testCart.getItems().add(item);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        CartDTO result = cartService.updateCartItemQuantity(1L, 1L, 4);

        assertNotNull(result);
        assertEquals(4, item.getQuantity());
        verify(cartRepository, times(1)).save(testCart);
    }

    @Test
    @DisplayName("Cập nhật số lượng vượt tồn kho ném BadRequestException")
    void updateCartItemQuantity_exceedStock_throwsBadRequest() {
        CartItem item = CartItem.builder()
                .id(1L)
                .cart(testCart)
                .book(bookWithoutDiscount) // stock is 10
                .quantity(1)
                .build();
        testCart.getItems().add(item);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));

        assertThrows(BadRequestException.class, () -> cartService.updateCartItemQuantity(1L, 1L, 20));
    }

    @Test
    @DisplayName("Xóa một sản phẩm theo itemId thành công")
    void removeCartItem_success() {
        CartItem item = CartItem.builder()
                .id(1L)
                .cart(testCart)
                .book(bookWithoutDiscount)
                .quantity(1)
                .build();
        testCart.getItems().add(item);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        cartService.removeCartItem(1L, 1L);

        assertTrue(testCart.getItems().isEmpty());
        verify(cartRepository, times(1)).save(testCart);
    }

    @Test
    @DisplayName("Xóa sạch toàn bộ giỏ hàng thành công")
    void clearCart_success() {
        CartItem item1 = CartItem.builder().id(1L).cart(testCart).book(bookWithoutDiscount).quantity(1).build();
        CartItem item2 = CartItem.builder().id(2L).cart(testCart).book(bookWithDiscount).quantity(2).build();
        testCart.getItems().addAll(List.of(item1, item2));

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        cartService.clearCart(1L);

        assertTrue(testCart.getItems().isEmpty());
        verify(cartRepository, times(1)).save(testCart);
    }
}
