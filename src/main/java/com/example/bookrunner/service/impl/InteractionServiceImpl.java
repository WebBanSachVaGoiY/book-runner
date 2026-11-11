package com.example.bookrunner.service.impl;

import com.example.bookrunner.dto.request.InteractionLogRequest;
import com.example.bookrunner.enums.InteractionType;
import com.example.bookrunner.model.Book;
import com.example.bookrunner.model.User;
import com.example.bookrunner.model.UserBookInteraction;
import com.example.bookrunner.repository.BookRepository;
import com.example.bookrunner.repository.UserBookInteractionRepository;
import com.example.bookrunner.repository.UserRepository;
import com.example.bookrunner.service.InteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionServiceImpl implements InteractionService {

    private final UserBookInteractionRepository interactionRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    // TTL Debounce: VIEW = 15 phút, ADD_TO_CART = 2 phút
    private static final long VIEW_DEBOUNCE_TTL_MS = 15 * 60 * 1000L;
    private static final long ADD_TO_CART_DEBOUNCE_TTL_MS = 2 * 60 * 1000L;

    // Giới hạn tần suất gọi API (Rate Limit): tối đa 60 requests / phút / user
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final long RATE_LIMIT_WINDOW_MS = 60 * 1000L;

    // In-memory Atomic Debounce Cache: Key = "userId:bookId:interactionType" -> Value = Timestamp (ms)
    private final Map<String, Long> debounceCache = new ConcurrentHashMap<>();

    // In-memory Rate Limiter: Key = userId -> Value = RateBucket
    private final Map<Long, RateBucket> rateLimitMap = new ConcurrentHashMap<>();

    private static class RateBucket {
        volatile long windowStartMs;
        final AtomicInteger counter = new AtomicInteger(0);

        RateBucket(long startMs) {
            this.windowStartMs = startMs;
            this.counter.set(1);
        }
    }

    @Async("interactionTaskExecutor")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logInteractionAsync(Long userId, InteractionLogRequest request) {
        doLogInteraction(userId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logInteraction(Long userId, InteractionLogRequest request) {
        doLogInteraction(userId, request);
    }

    /**
     * Logic ghi nhận tương tác dùng chung.
     * <p>
     * Tách riêng khỏi các phương thức public để tránh self-invocation: nếu
     * {@code logInteractionAsync} gọi trực tiếp {@code this.logInteraction(...)} thì
     * {@code @Transactional} sẽ bị bỏ qua (không đi qua proxy của Spring).
     */
    private void doLogInteraction(Long userId, InteractionLogRequest request) {
        if (userId == null || request == null || request.getBookId() == null || request.getInteractionType() == null) {
            log.warn("Bỏ qua log interaction: tham số không hợp lệ (userId={}, request={})", userId, request);
            return;
        }

        // 1. Chỉ chấp nhận VIEW và ADD_TO_CART qua endpoint này (BUY và RATING sinh tự động từ hệ thống)
        if (request.getInteractionType() != InteractionType.VIEW && request.getInteractionType() != InteractionType.ADD_TO_CART) {
            log.warn("Từ chối ghi nhận tương tác '{}' qua endpoint người dùng (chỉ chấp nhận VIEW hoặc ADD_TO_CART)",
                    request.getInteractionType());
            return;
        }

        // 2. Rate Limiting kiểm soát lạm dụng (chặn bot/script spam)
        if (isRateLimited(userId)) {
            log.warn("Rate limit vượt ngưỡng: User {} gửi quá {} requests/phút. Bỏ qua event.",
                    userId, MAX_REQUESTS_PER_MINUTE);
            return;
        }

        // 3. Atomic In-Memory Debounce (Giải quyết triệt để Race Condition & Chống trùng lặp F5)
        long now = System.currentTimeMillis();
        long ttl = (request.getInteractionType() == InteractionType.VIEW) ? VIEW_DEBOUNCE_TTL_MS : ADD_TO_CART_DEBOUNCE_TTL_MS;
        String cacheKey = userId + ":" + request.getBookId() + ":" + request.getInteractionType().name();

        boolean shouldProceed = debounceCache.compute(cacheKey, (key, lastRecordedTime) -> {
            if (lastRecordedTime != null && (now - lastRecordedTime) < ttl) {
                return lastRecordedTime; // Vẫn trong cửa sổ debounce -> Giữ nguyên, không cập nhật
            }
            return now; // Hết hạn hoặc lần đầu tương tác -> Cập nhật timestamp mới
        }) == now;

        if (!shouldProceed) {
            log.debug("Debounce (Atomic): Bỏ qua interaction {} cho user {} và book {} (còn trong cửa sổ {}s)",
                    request.getInteractionType(), userId, request.getBookId(), ttl / 1000);
            return;
        }

        // Tự động dọn dẹp cache nếu kích thước quá lớn (> 20.000 entries)
        cleanCacheIfNecessary(now);

        // 4. Kiểm tra sự tồn tại của User và Book
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Không tìm thấy user id {} để ghi interaction", userId);
            return;
        }

        Book book = bookRepository.findById(request.getBookId()).orElse(null);
        if (book == null) {
            log.warn("Không tìm thấy book id {} để ghi interaction", request.getBookId());
            return;
        }

        // Không ghi nhận tương tác cho sách đã ngừng kinh doanh
        if (!Boolean.TRUE.equals(book.getActive())) {
            log.debug("Sách '{}' (id={}) đã ngừng kinh doanh, bỏ qua ghi interaction", book.getTitle(), book.getId());
            return;
        }

        // 5. Lưu Raw Event vào MariaDB
        UserBookInteraction interaction = UserBookInteraction.builder()
                .user(user)
                .book(book)
                .interactionType(request.getInteractionType())
                .weight(request.getInteractionType().getDefaultWeight())
                .build();

        interactionRepository.save(interaction);
        log.info("Đã ghi nhận tương tác {}: User='{}', Book='{}' (id={})",
                request.getInteractionType(), user.getUsername(), book.getTitle(), book.getId());
    }

    @Override
    public void clearDebounceCache() {
        debounceCache.clear();
        rateLimitMap.clear();
    }

    private boolean isRateLimited(Long userId) {
        long now = System.currentTimeMillis();
        RateBucket bucket = rateLimitMap.compute(userId, (id, existing) -> {
            if (existing == null || (now - existing.windowStartMs) >= RATE_LIMIT_WINDOW_MS) {
                return new RateBucket(now);
            }
            existing.counter.incrementAndGet();
            return existing;
        });

        return bucket.counter.get() > MAX_REQUESTS_PER_MINUTE;
    }

    private void cleanCacheIfNecessary(long now) {
        if (debounceCache.size() > 20000) {
            debounceCache.entrySet().removeIf(entry -> (now - entry.getValue()) > VIEW_DEBOUNCE_TTL_MS);
        }
        if (rateLimitMap.size() > 5000) {
            rateLimitMap.entrySet().removeIf(entry -> (now - entry.getValue().windowStartMs) > RATE_LIMIT_WINDOW_MS);
        }
    }
}
