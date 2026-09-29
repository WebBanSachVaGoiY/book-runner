package com.example.bookrunner.service.impl;

import com.example.bookrunner.dto.request.CreateOrderRequest;
import com.example.bookrunner.dto.request.OrderItemRequest;
import com.example.bookrunner.dto.response.OrderItemResponseDTO;
import com.example.bookrunner.dto.response.OrderResponseDTO;
import com.example.bookrunner.enums.InteractionType;
import com.example.bookrunner.enums.OrderStatus;
import com.example.bookrunner.enums.PaymentMethod;
import com.example.bookrunner.enums.PaymentStatus;
import com.example.bookrunner.exception.BadRequestException;
import com.example.bookrunner.exception.ItemNotFoundException;
import com.example.bookrunner.model.*;
import com.example.bookrunner.repository.*;
import com.example.bookrunner.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final UserBookInteractionRepository interactionRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponseDTO createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy người dùng với id: " + userId));

        // 1. Xác định danh sách sản phẩm cần mua
        List<OrderItemRequest> purchaseItems = new ArrayList<>();
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            purchaseItems = request.getItems();
        } else {
            // Lấy từ giỏ hàng nếu request không truyền danh sách items
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new BadRequestException("Giỏ hàng của bạn đang trống"));
            if (cart.getItems() == null || cart.getItems().isEmpty()) {
                throw new BadRequestException("Giỏ hàng của bạn đang trống, vui lòng chọn sách trước khi đặt hàng");
            }
            for (CartItem ci : cart.getItems()) {
                purchaseItems.add(new OrderItemRequest(ci.getBook().getId(), ci.getQuantity()));
            }
        }

        // 2. Khởi tạo đối tượng Order
        String orderCode = "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        PaymentMethod paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.COD;

        Order order = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .recipientName(request.getRecipientName().trim())
                .recipientPhone(request.getRecipientPhone().trim())
                .shippingAddress(request.getShippingAddress().trim())
                .note(request.getNote())
                .status(OrderStatus.PENDING)
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.PENDING)
                .shippingFee(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;
        Set<Long> processedBookIds = new HashSet<>();

        // 3. Trừ kho an toàn (Pessimistic Write Lock) và tính giá
        for (OrderItemRequest itemReq : purchaseItems) {
            if (itemReq.getBookId() == null || itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new BadRequestException("Thông tin sản phẩm đặt hàng không hợp lệ");
            }

            // Ngăn chặn trùng bookId trong cùng một request
            if (!processedBookIds.add(itemReq.getBookId())) {
                throw new BadRequestException("Sách với ID " + itemReq.getBookId() + " bị trùng lặp trong đơn hàng");
            }

            Book book = bookRepository.findByIdWithLock(itemReq.getBookId())
                    .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy sách với id: " + itemReq.getBookId()));

            if (!Boolean.TRUE.equals(book.getActive())) {
                throw new BadRequestException("Sách '" + book.getTitle() + "' hiện đang ngừng kinh doanh");
            }

            if (book.getStockQuantity() < itemReq.getQuantity()) {
                throw new BadRequestException("Sách '" + book.getTitle() + "' không đủ số lượng tồn kho (còn lại: "
                        + book.getStockQuantity() + ", yêu cầu: " + itemReq.getQuantity() + ")");
            }

            // Trừ tồn kho an toàn và tăng số lượng bán
            book.setStockQuantity(book.getStockQuantity() - itemReq.getQuantity());
            int currentSold = book.getSoldCount() != null ? book.getSoldCount() : 0;
            book.setSoldCount(currentSold + itemReq.getQuantity());
            bookRepository.save(book);

            // Xác định đơn giá (ưu tiên discountPrice nếu có)
            BigDecimal unitPrice = (book.getDiscountPrice() != null && book.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0)
                    ? book.getDiscountPrice()
                    : book.getPrice();

            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .book(book)
                    .quantity(itemReq.getQuantity())
                    .price(unitPrice)
                    .build();

            order.getItems().add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        order.setFinalAmount(totalAmount.add(order.getShippingFee()));

        Order savedOrder = orderRepository.save(order);

        // 4. Xóa các sản phẩm đã mua khỏi giỏ hàng của user
        Optional<Cart> optionalCart = cartRepository.findByUserId(userId);
        if (optionalCart.isPresent()) {
            Cart cart = optionalCart.get();
            if (cart.getItems() != null && !cart.getItems().isEmpty()) {
                cart.getItems().removeIf(ci -> processedBookIds.contains(ci.getBook().getId()));
                cartRepository.save(cart);
            }
        }

        // 5. Ghi nhận tương tác BUY vào RecSys (user_book_interactions)
        for (OrderItem oi : savedOrder.getItems()) {
            try {
                UserBookInteraction interaction = UserBookInteraction.builder()
                        .user(user)
                        .book(oi.getBook())
                        .interactionType(InteractionType.BUY)
                        .weight(InteractionType.BUY.getDefaultWeight())
                        .build();
                interactionRepository.save(interaction);
            } catch (Exception e) {
                log.warn("Không thể lưu interaction BUY cho user {} và book {}: {}",
                        userId, oi.getBook().getId(), e.getMessage());
            }
        }

        log.info("Tạo đơn hàng thành công: orderCode={}, user={}, finalAmount={}",
                savedOrder.getOrderCode(), user.getUsername(), savedOrder.getFinalAmount());

        return mapToDTO(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getMyOrders(Long userId, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage;
        if (status != null) {
            orderPage = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        } else {
            orderPage = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return orderPage.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderDetail(Long userId, Long orderId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

        if (!isAdmin && !order.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập đơn hàng này");
        }

        return mapToDTO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponseDTO cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền hủy đơn hàng này");
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Không thể hủy đơn hàng ở trạng thái hiện tại: " + order.getStatus()
                    + ". Chỉ đơn hàng CHỜ XỬ LÝ hoặc ĐÃ XÁC NHẬN mới được phép hủy.");
        }

        // Hoàn lại số lượng tồn kho và giảm số lượng bán
        for (OrderItem oi : order.getItems()) {
            Book book = bookRepository.findByIdWithLock(oi.getBook().getId()).orElse(null);
            if (book != null) {
                book.setStockQuantity(book.getStockQuantity() + oi.getQuantity());
                int currentSold = book.getSoldCount() != null ? book.getSoldCount() : 0;
                book.setSoldCount(Math.max(0, currentSold - oi.getQuantity()));
                bookRepository.save(book);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);

        log.info("Hủy đơn hàng thành công: orderCode={}, user={}", order.getOrderCode(), userId);
        return mapToDTO(updatedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getAllOrdersForAdmin(OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage;
        if (status != null) {
            orderPage = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            orderPage = orderRepository.findAll(pageable);
        }
        return orderPage.map(this::mapToDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponseDTO updateOrderStatusForAdmin(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

        OrderStatus oldStatus = order.getStatus();

        // Nếu admin chuyển sang CANCELLED và trước đó chưa hủy -> hoàn lại tồn kho và giảm soldCount
        if (newStatus == OrderStatus.CANCELLED && oldStatus != OrderStatus.CANCELLED) {
            for (OrderItem oi : order.getItems()) {
                Book book = bookRepository.findByIdWithLock(oi.getBook().getId()).orElse(null);
                if (book != null) {
                    book.setStockQuantity(book.getStockQuantity() + oi.getQuantity());
                    int currentSold = book.getSoldCount() != null ? book.getSoldCount() : 0;
                    book.setSoldCount(Math.max(0, currentSold - oi.getQuantity()));
                    bookRepository.save(book);
                }
            }
        }

        // Nếu giao hàng thành công (DELIVERED) và là COD -> cập nhật đã thanh toán
        if (newStatus == OrderStatus.DELIVERED && order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        log.info("Admin cập nhật trạng thái đơn {}: {} -> {}", order.getOrderCode(), oldStatus, newStatus);
        return mapToDTO(savedOrder);
    }

    private OrderResponseDTO mapToDTO(Order order) {
        List<OrderItemResponseDTO> itemDTOs = order.getItems().stream().map(oi -> {
            BigDecimal subtotal = oi.getPrice().multiply(BigDecimal.valueOf(oi.getQuantity()));
            return OrderItemResponseDTO.builder()
                    .id(oi.getId())
                    .bookId(oi.getBook().getId())
                    .bookTitle(oi.getBook().getTitle())
                    .bookAuthor(oi.getBook().getAuthor())
                    .coverImageUrl(oi.getBook().getCoverImageUrl())
                    .quantity(oi.getQuantity())
                    .price(oi.getPrice())
                    .subtotal(subtotal)
                    .build();
        }).collect(Collectors.toList());

        return OrderResponseDTO.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUser().getId())
                .username(order.getUser().getUsername())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingAddress(order.getShippingAddress())
                .note(order.getNote())
                .totalAmount(order.getTotalAmount())
                .shippingFee(order.getShippingFee())
                .finalAmount(order.getFinalAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .items(itemDTOs)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
