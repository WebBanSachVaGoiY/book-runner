package com.example.bookrunner.service;

import com.example.bookrunner.dto.request.CreateOrderRequest;
import com.example.bookrunner.dto.request.OrderItemRequest;
import com.example.bookrunner.dto.response.OrderResponseDTO;
import com.example.bookrunner.enums.InteractionType;
import com.example.bookrunner.enums.OrderStatus;
import com.example.bookrunner.enums.PaymentMethod;
import com.example.bookrunner.enums.PaymentStatus;
import com.example.bookrunner.enums.Role;
import com.example.bookrunner.exception.BadRequestException;
import com.example.bookrunner.exception.ItemNotFoundException;
import com.example.bookrunner.model.*;
import com.example.bookrunner.repository.*;
import com.example.bookrunner.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserBookInteractionRepository interactionRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testUser;
    private Book testBook;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("customer01")
                .email("customer01@example.com")
                .role(Role.ROLE_CUSTOMER)
                .build();

        testBook = Book.builder()
                .id(10L)
                .title("Lập trình Java căn bản")
                .author("Nguyễn Văn B")
                .price(new BigDecimal("200000"))
                .discountPrice(new BigDecimal("180000"))
                .stockQuantity(15)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("createOrder - Thành công với danh sách items, trừ tồn kho và ghi nhận interaction BUY")
    void createOrder_Success_WithExplicitItems() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Trần Văn C")
                .recipientPhone("0987654321")
                .shippingAddress("123 Cầu Giấy, Hà Nội")
                .note("Giao giờ hành chính")
                .paymentMethod(PaymentMethod.COD)
                .items(List.of(new OrderItemRequest(10L, 2)))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookRepository.findByIdWithLock(10L)).thenReturn(Optional.of(testBook));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });

        OrderResponseDTO response = orderService.createOrder(1L, request);

        assertNotNull(response);
        assertEquals("Trần Văn C", response.getRecipientName());
        assertEquals("0987654321", response.getRecipientPhone());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(PaymentMethod.COD, response.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, response.getPaymentStatus());
        // Giá discountPrice = 180,000 * 2 = 360,000
        assertEquals(new BigDecimal("360000"), response.getTotalAmount());
        assertEquals(new BigDecimal("360000"), response.getFinalAmount());
        assertEquals(1, response.getItems().size());
        assertEquals(2, response.getItems().get(0).getQuantity());

        // Kiểm tra tồn kho giảm từ 15 còn 13
        assertEquals(13, testBook.getStockQuantity());
        verify(bookRepository, times(1)).save(testBook);

        // Kiểm tra interaction BUY được ghi nhận
        verify(interactionRepository, times(1)).save(argThat(interaction ->
                interaction.getUser().getId().equals(1L) &&
                interaction.getBook().getId().equals(10L) &&
                interaction.getInteractionType() == InteractionType.BUY
        ));
    }

    @Test
    @DisplayName("createOrder - Báo lỗi khi tồn kho không đủ")
    void createOrder_ThrowsException_WhenOutOfStock() {
        testBook.setStockQuantity(1);

        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Trần Văn C")
                .recipientPhone("0987654321")
                .shippingAddress("123 Cầu Giấy, Hà Nội")
                .items(List.of(new OrderItemRequest(10L, 5)))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookRepository.findByIdWithLock(10L)).thenReturn(Optional.of(testBook));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> orderService.createOrder(1L, request));

        assertTrue(ex.getMessage().contains("không đủ số lượng tồn kho"));
        // Không lưu đơn và không trừ kho
        assertEquals(1, testBook.getStockQuantity());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder - Báo lỗi khi sách ngừng kinh doanh")
    void createOrder_ThrowsException_WhenBookInactive() {
        testBook.setActive(false);

        CreateOrderRequest request = CreateOrderRequest.builder()
                .recipientName("Trần Văn C")
                .recipientPhone("0987654321")
                .shippingAddress("123 Cầu Giấy, Hà Nội")
                .items(List.of(new OrderItemRequest(10L, 1)))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookRepository.findByIdWithLock(10L)).thenReturn(Optional.of(testBook));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> orderService.createOrder(1L, request));

        assertTrue(ex.getMessage().contains("ngừng kinh doanh"));
    }

    @Test
    @DisplayName("cancelOrder - Thành công khi đơn ở trạng thái PENDING và hoàn lại tồn kho")
    void cancelOrder_Success() {
        OrderItem item = OrderItem.builder()
                .id(1L)
                .book(testBook)
                .quantity(3)
                .price(new BigDecimal("180000"))
                .build();

        Order order = Order.builder()
                .id(50L)
                .orderCode("ORD-TEST-123")
                .user(testUser)
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>(List.of(item)))
                .build();

        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(bookRepository.findByIdWithLock(10L)).thenReturn(Optional.of(testBook));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDTO response = orderService.cancelOrder(1L, 50L);

        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        // Tồn kho ban đầu là 15, hoàn lại 3 -> 18
        assertEquals(18, testBook.getStockQuantity());
        verify(bookRepository, times(1)).save(testBook);
    }

    @Test
    @DisplayName("cancelOrder - Báo lỗi khi đơn hàng đã SHIPPING hoặc DELIVERED")
    void cancelOrder_ThrowsException_WhenStatusNotAllowed() {
        Order order = Order.builder()
                .id(50L)
                .orderCode("ORD-TEST-123")
                .user(testUser)
                .status(OrderStatus.SHIPPING)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> orderService.cancelOrder(1L, 50L));

        assertTrue(ex.getMessage().contains("Không thể hủy đơn hàng"));
    }

    @Test
    @DisplayName("cancelOrder - Báo lỗi AccessDenied khi user không phải chủ đơn hàng")
    void cancelOrder_ThrowsException_WhenNotOwner() {
        Order order = Order.builder()
                .id(50L)
                .orderCode("ORD-TEST-123")
                .user(User.builder().id(999L).username("other").build())
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class,
                () -> orderService.cancelOrder(1L, 50L));
    }

    @Test
    @DisplayName("updateOrderStatusForAdmin - Tự động cập nhật paymentStatus=PAID khi DELIVERED với đơn COD")
    void adminUpdateStatus_Delivered_CodUpdatesPaymentStatus() {
        Order order = Order.builder()
                .id(50L)
                .orderCode("ORD-TEST-123")
                .user(testUser)
                .status(OrderStatus.SHIPPING)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDTO response = orderService.updateOrderStatusForAdmin(50L, OrderStatus.DELIVERED);

        assertEquals(OrderStatus.DELIVERED, response.getStatus());
        assertEquals(PaymentStatus.PAID, response.getPaymentStatus());
    }
}
