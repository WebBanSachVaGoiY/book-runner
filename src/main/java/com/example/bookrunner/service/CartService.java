package com.example.bookrunner.service;

import com.example.bookrunner.dto.CartDTO;
import com.example.bookrunner.exception.BadRequestException;
import com.example.bookrunner.exception.ItemNotFoundException;
import com.example.bookrunner.model.Book;
import com.example.bookrunner.model.Cart;
import com.example.bookrunner.model.CartItem;
import com.example.bookrunner.repository.BookRepository;
import com.example.bookrunner.repository.CartRepository;
import com.example.bookrunner.util.CartToCartDTO;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Transactional
@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private BookRepository bookRepository;

    public CartDTO getCart(Long userId) {
        Optional<Cart> cart = cartRepository.findByUserId(userId);
        if (cart.isEmpty())
            throw new ItemNotFoundException("Không tìm thấy giỏ hàng");
        return CartToCartDTO.toCartDTO(cart.get());
    }

    public void addEditCart(Long userId, Long bookId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Số lượng mua phải lớn hơn 0!");
        }
        Optional<Book> book = bookRepository.findById(bookId);
        if (book.isEmpty())
            throw new ItemNotFoundException("Không tìm thấy sách!");
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy giỏ hàng"));
        Optional<CartItem> item = cart.getItems().stream()
                .filter(bookItem -> bookItem.getBook().getId().equals(bookId)).findFirst();
        int targetQuantity = quantity;

        if (item.isPresent()) {
            targetQuantity += item.get().getQuantity();
        }

        if (targetQuantity > book.get().getStockQuantity()) {
            throw new BadRequestException("Số lượng đặt mua vượt quá tồn kho!");
        }

        if (item.isPresent()) {
            item.get().setQuantity(targetQuantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setBook(book.get());
            newItem.setQuantity(quantity);
            cart.getItems().add(newItem);
        }
        cartRepository.save(cart);
    }

    public void deleteCartItem(Long userId, List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất một sản phẩm để xoá!");
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy giỏ hàng"));

        List<CartItem> itemsToRemove = cart.getItems().stream()
                .filter(item -> bookIds.contains(item.getBook().getId()))
                .toList();

        if (itemsToRemove.isEmpty()) {
            throw new ItemNotFoundException("Không tìm thấy sản phẩm nào trong giỏ hàng để xoá!");
        }

        cart.getItems().removeAll(itemsToRemove);
        cartRepository.save(cart);
    }

    public void addToCart(Long userId, com.example.bookrunner.dto.request.AddToCartRequest request) {
        addEditCart(userId, request.getBookId(), request.getQuantity());
    }

    public CartDTO updateCartItemQuantity(Long userId, Long itemId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Số lượng mua phải lớn hơn 0!");
        }
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy giỏ hàng"));

        CartItem item = cart.getItems().stream()
                .filter(ci -> ci.getId().equals(itemId) || ci.getBook().getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy sản phẩm trong giỏ hàng"));

        if (quantity > item.getBook().getStockQuantity()) {
            throw new BadRequestException("Số lượng đặt mua vượt quá tồn kho!");
        }

        item.setQuantity(quantity);
        cartRepository.save(cart);
        return CartToCartDTO.toCartDTO(cart);
    }

    public void removeCartItem(Long userId, Long itemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy giỏ hàng"));

        CartItem item = cart.getItems().stream()
                .filter(ci -> ci.getId().equals(itemId) || ci.getBook().getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy sản phẩm nào trong giỏ hàng để xoá!"));

        cart.getItems().remove(item);
        cartRepository.save(cart);
    }

    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy giỏ hàng"));

        cart.getItems().clear();
        cartRepository.save(cart);
    }
}
