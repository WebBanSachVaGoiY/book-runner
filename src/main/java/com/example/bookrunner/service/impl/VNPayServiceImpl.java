package com.example.bookrunner.service.impl;

import com.example.bookrunner.config.VNPayConfig;
import com.example.bookrunner.dto.response.PaymentCallbackDTO;
import com.example.bookrunner.dto.response.VNPayPaymentResponseDTO;
import com.example.bookrunner.enums.InteractionType;
import com.example.bookrunner.enums.OrderStatus;
import com.example.bookrunner.enums.PaymentMethod;
import com.example.bookrunner.enums.PaymentStatus;
import com.example.bookrunner.exception.BadRequestException;
import com.example.bookrunner.exception.ItemNotFoundException;
import com.example.bookrunner.model.Book;
import com.example.bookrunner.model.Cart;
import com.example.bookrunner.model.Order;
import com.example.bookrunner.model.OrderItem;
import com.example.bookrunner.model.UserBookInteraction;
import com.example.bookrunner.repository.BookRepository;
import com.example.bookrunner.repository.CartRepository;
import com.example.bookrunner.repository.OrderRepository;
import com.example.bookrunner.repository.UserBookInteractionRepository;
import com.example.bookrunner.service.VNPayService;
import com.example.bookrunner.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayServiceImpl implements VNPayService {

    private final VNPayConfig vnPayConfig;
    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final CartRepository cartRepository;
    private final UserBookInteractionRepository interactionRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VNPayPaymentResponseDTO createPaymentUrl(Long userId, Long orderId, HttpServletRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

        if (userId != null && !order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền thanh toán cho đơn hàng này!");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Đơn hàng này đã được thanh toán thành công trước đó!");
        }

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.RETURNED) {
            throw new BadRequestException("Không thể thanh toán cho đơn hàng đã bị hủy hoặc hoàn trả!");
        }

        // Đảm bảo paymentMethod là VNPAY
        if (order.getPaymentMethod() != PaymentMethod.VNPAY) {
            order.setPaymentMethod(PaymentMethod.VNPAY);
            orderRepository.save(order);
        }

        // VNPay quy định số tiền phải nhân với 100 (bỏ phần thập phân)
        long vnpAmount = order.getFinalAmount().multiply(BigDecimal.valueOf(100)).longValue();

        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);
        String createDate = now.format(DATE_FORMATTER);
        String expireDate = now.plusMinutes(15).format(DATE_FORMATTER);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", order.getOrderCode());
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderCode());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));
        vnpParams.put("vnp_CreateDate", createDate);
        vnpParams.put("vnp_ExpireDate", expireDate);

        String[] hashAndQuery = VNPayUtil.buildHashAndQuery(vnpParams);
        String hashData = hashAndQuery[0];
        String queryUrl = hashAndQuery[1];
        String secureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);
        String paymentUrl = vnPayConfig.getUrl() + "?" + queryUrl + "&vnp_SecureHash=" + secureHash;

        log.info("Tạo VNPay paymentUrl thành công cho đơn hàng: orderCode={}, amount={}", order.getOrderCode(), vnpAmount);

        return VNPayPaymentResponseDTO.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .amount(order.getFinalAmount())
                .paymentUrl(paymentUrl)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentCallbackDTO processCallback(Map<String, String> queryParams) {
        log.info("Nhận callback chuyển hướng từ VNPay: {}", queryParams);

        boolean isValidSignature = VNPayUtil.verifySignature(queryParams, vnPayConfig.getHashSecret());
        String orderCode = queryParams.get("vnp_TxnRef");
        String responseCode = queryParams.get("vnp_ResponseCode");
        String transactionNo = queryParams.get("vnp_TransactionNo");
        String bankCode = queryParams.get("vnp_BankCode");
        String rawAmount = queryParams.get("vnp_Amount");
        BigDecimal amount = rawAmount != null ? new BigDecimal(rawAmount).divide(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

        if (!isValidSignature) {
            log.warn("Chữ ký VNPay không hợp lệ: orderCode={}", orderCode);
            return PaymentCallbackDTO.builder()
                    .success(false)
                    .orderCode(orderCode)
                    .responseCode(responseCode)
                    .message("Chữ ký giao dịch không hợp lệ (Invalid Signature)")
                    .build();
        }

        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            log.warn("Không tìm thấy đơn hàng với mã: {}", orderCode);
            return PaymentCallbackDTO.builder()
                    .success(false)
                    .orderCode(orderCode)
                    .responseCode(responseCode)
                    .message("Không tìm thấy đơn hàng trong hệ thống")
                    .build();
        }

        boolean isSuccess = "00".equals(responseCode);
        if (isSuccess) {
            finalizeSuccessfulPayment(order);
            log.info("Cập nhật đơn hàng sang PAID thành công qua Return Callback: orderCode={}", orderCode);

            return PaymentCallbackDTO.builder()
                    .success(true)
                    .orderId(order.getId())
                    .orderCode(orderCode)
                    .transactionNo(transactionNo)
                    .amount(amount)
                    .bankCode(bankCode)
                    .responseCode(responseCode)
                    .message("Thanh toán đơn hàng thành công")
                    .build();
        } else {
            // Thanh toán không thành công / Khách hàng hủy:
            // 1. Hoàn lại số lượng tồn kho đã trừ tạm tính
            reverseStockAndSoldCount(order);
            // 2. Xóa đơn hàng khỏi hệ thống (đơn hàng không được tạo)
            orderRepository.delete(order);
            log.info("Thanh toán VNPay không thành công (mã {}), đã hoàn tồn kho và không khởi tạo đơn hàng: orderCode={}", responseCode, orderCode);

            return PaymentCallbackDTO.builder()
                    .success(false)
                    .orderId(null)
                    .orderCode(orderCode)
                    .transactionNo(transactionNo)
                    .amount(amount)
                    .bankCode(bankCode)
                    .responseCode(responseCode)
                    .message(getVNPayResponseMessage(responseCode))
                    .build();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> processIpn(Map<String, String> queryParams) {
        log.info("Nhận Webhook IPN từ máy chủ VNPay: {}", queryParams);
        Map<String, String> response = new HashMap<>();

        // 1. Kiểm tra chữ ký số
        boolean isValidSignature = VNPayUtil.verifySignature(queryParams, vnPayConfig.getHashSecret());
        if (!isValidSignature) {
            log.warn("[VNPay IPN] Chữ ký không hợp lệ");
            response.put("RspCode", "97");
            response.put("Message", "Invalid Checksum");
            return response;
        }

        // 2. Kiểm tra sự tồn tại của đơn hàng
        String orderCode = queryParams.get("vnp_TxnRef");
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            log.warn("[VNPay IPN] Không tìm thấy đơn hàng: orderCode={}", orderCode);
            response.put("RspCode", "01");
            response.put("Message", "Order not found");
            return response;
        }

        // 3. Kiểm tra số tiền
        String rawAmount = queryParams.get("vnp_Amount");
        long vnpAmount = rawAmount != null ? Long.parseLong(rawAmount) / 100 : 0;
        long orderAmount = order.getFinalAmount().longValue();
        if (vnpAmount != orderAmount) {
            log.warn("[VNPay IPN] Số tiền không khớp: DB={}, VNPay={}", orderAmount, vnpAmount);
            response.put("RspCode", "04");
            response.put("Message", "Invalid Amount");
            return response;
        }

        // 4. Kiểm tra tính lũy đẳng (nếu đã PAID rồi thì không cập nhật lại)
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.info("[VNPay IPN] Đơn hàng đã được xác nhận thanh toán trước đó: orderCode={}", orderCode);
            response.put("RspCode", "02");
            response.put("Message", "Order already confirmed");
            return response;
        }

        // 5. Cập nhật trạng thái
        String responseCode = queryParams.get("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            finalizeSuccessfulPayment(order);
            log.info("[VNPay IPN] Cập nhật thành công đơn hàng sang PAID: orderCode={}", orderCode);
        } else {
            reverseStockAndSoldCount(order);
            orderRepository.delete(order);
            log.info("[VNPay IPN] Giao dịch thất bại (mã {}), đã hoàn tồn kho và không khởi tạo đơn hàng: orderCode={}", responseCode, orderCode);
        }

        response.put("RspCode", "00");
        response.put("Message", "Confirm Success");
        return response;
    }

    private void finalizeSuccessfulPayment(Order order) {
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.PAID);
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CONFIRMED);
            }
            orderRepository.save(order);
        }

        // 1. Xóa các sản phẩm đã mua khỏi giỏ hàng
        try {
            if (order.getUser() != null) {
                Optional<Cart> optionalCart = cartRepository.findByUserId(order.getUser().getId());
                if (optionalCart.isPresent()) {
                    Cart cart = optionalCart.get();
                    if (cart.getItems() != null && !cart.getItems().isEmpty() && order.getItems() != null) {
                        Set<Long> bookIdsInOrder = order.getItems().stream()
                                .map(oi -> oi.getBook().getId())
                                .collect(Collectors.toSet());
                        cart.getItems().removeIf(ci -> bookIdsInOrder.contains(ci.getBook().getId()));
                        cartRepository.save(cart);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không thể xóa sản phẩm khỏi giỏ hàng sau khi thanh toán thành công: {}", e.getMessage());
        }

        // 2. Ghi nhận tương tác BUY vào RecSys
        if (order.getItems() != null && order.getUser() != null) {
            for (OrderItem oi : order.getItems()) {
                try {
                    UserBookInteraction interaction = UserBookInteraction.builder()
                            .user(order.getUser())
                            .book(oi.getBook())
                            .interactionType(InteractionType.BUY)
                            .weight(InteractionType.BUY.getDefaultWeight())
                            .build();
                    interactionRepository.save(interaction);
                } catch (Exception e) {
                    log.warn("Không thể lưu interaction BUY cho user {} và book {}: {}",
                            order.getUser().getId(), oi.getBook().getId(), e.getMessage());
                }
            }
        }
    }

    private void reverseStockAndSoldCount(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }
        for (OrderItem oi : order.getItems()) {
            if (oi.getBook() != null && oi.getBook().getId() != null) {
                Book book = bookRepository.findByIdWithLock(oi.getBook().getId()).orElse(null);
                if (book != null) {
                    book.setStockQuantity(book.getStockQuantity() + oi.getQuantity());
                    int currentSold = book.getSoldCount() != null ? book.getSoldCount() : 0;
                    book.setSoldCount(Math.max(0, currentSold - oi.getQuantity()));
                    bookRepository.save(book);
                }
            }
        }
    }

    private String getVNPayResponseMessage(String responseCode) {
        if (responseCode == null) return "Giao dịch không thành công";
        return switch (responseCode) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).";
            case "09" -> "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking.";
            case "10" -> "Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần.";
            case "11" -> "Đã hết hạn chờ thanh toán. Vui lòng thử lại.";
            case "12" -> "Thẻ/Tài khoản bị khóa.";
            case "24" -> "Khách hàng đã hủy giao dịch.";
            case "51" -> "Tài khoản không đủ số dư để thực hiện giao dịch.";
            case "65" -> "Tài khoản đã vượt quá hạn mức giao dịch trong ngày.";
            case "75" -> "Ngân hàng thanh toán đang bảo trì.";
            case "97" -> "Chữ ký giao dịch không hợp lệ.";
            default -> "Giao dịch thất bại (Mã phản hồi: " + responseCode + ")";
        };
    }
}
