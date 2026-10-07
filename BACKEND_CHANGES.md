# NHẬT KÝ CHI TIẾT CÁC THAY ĐỔI CODE BACK END (`book-runner`)

> **Dự án:** Hệ thống E-Commerce Bán Sách BookRunner (Spring Boot / MariaDB / Hibernate)  
> **Phạm vi cập nhật:** Sửa lỗi logic ISBN, loại bỏ rủi ro Cascade Category, thiết lập State Machine khép kín cho Đơn hàng, chuyển sang Bulk Soft-Delete Sách (@Modifying), đồng bộ cờ `active`, chuẩn hóa GlobalExceptionHandler về `ApiResponse`, áp dụng Bean Validation và chuyển sang Constructor Injection.  
> **Thời gian thực hiện:** 07/10/2026  
> **Trạng thái:** Đã kiểm thử, biên dịch thành công (`./gradlew compileJava` - 0 lỗi; 100% Service Unit Tests Passed)

---

## 📌 1. TỔNG QUAN NGUYÊN TẮC THAY ĐỔI

1. **Khắc phục triệt để lỗi kiểm tra trùng ISBN:** Phân biệt rõ giữa kiểm tra ISBN của chính cuốn sách đang sửa và ISBN của cuốn sách khác (`existsByIsbnAndIdNot`). Giữ nguyên kiểm tra `findByIsbn` toàn cục (cả active và inactive) khi tạo mới để bảo toàn ràng buộc `UNIQUE` ở tầng Database MariaDB.
2. **Triệt tiêu nguy cơ xóa dây chuyền mất sách (Zero Cascade Danger):** Gỡ bỏ `CascadeType.ALL` ở quan hệ `Category -> Book`. Chặn cứng thao tác xóa danh mục nếu danh mục đó đang chứa sách (`existsByCategoryIdIn`).
3. **Thiết lập Máy trạng thái (State Machine Matrix) cho Đơn hàng:** 
   - Quản lý vòng đời đơn hàng qua ma trận `TRANSITIONS` cố định (`PENDING` $\rightarrow$ `CONFIRMED`/`CANCELLED`; `CONFIRMED` $\rightarrow$ `SHIPPING`/`CANCELLED`; `SHIPPING` $\rightarrow$ `DELIVERED`/`RETURNED`; `DELIVERED` $\rightarrow$ `RETURNED`).
   - Hỗ trợ tính lũy đẳng (Idempotent): bỏ qua nếu trạng thái gửi lên không thay đổi (`newStatus == oldStatus`).
   - Tự động hoàn kho và giảm `soldCount` khi chuyển sang `CANCELLED` hoặc `RETURNED`.
   - Tự động cập nhật `PaymentStatus`: chuyển COD sang `PAID` khi giao thành công, và chuyển online payment sang `REFUNDED` khi đơn bị hủy/trả.
4. **Xóa mềm (Soft-Delete) Sách chuẩn kỹ thuật:**
   - Thay thế xóa cứng SQL bằng câu query cập nhật lô: `@Modifying(clearAutomatically = true, flushAutomatically = true)`. Xóa sạch Persistence Context L1 Cache của Hibernate để ngăn chặn tình trạng dirty-checking vô tình ghi đè lại dữ liệu cũ.
   - Bổ sung endpoint `PATCH /api/v1/books/restore` cho phép Admin khôi phục lại các sách đã ngừng kinh doanh.
5. **Bảo vệ toàn vẹn cờ `active` trên toàn hệ thống:**
   - Chặn người dùng thêm sách ngừng kinh doanh vào giỏ hàng ngay tại tầng `CartService`.
6. **Chuẩn hóa cấu trúc phản hồi lỗi (API Error Consistency):**
   - Đưa tất cả Exception (`ItemNotFoundException`, `DuplicateUniqueFieldException`, `FieldRequiredException`) về chuẩn phản hồi chung `ResponseEntity<ApiResponse<Void>>`, giúp Frontend Axios Interceptor hiển thị đúng thông điệp qua Toast UI.
7. **Tuân thủ Clean Code & Best Practices của Spring Boot:**
   - Bổ sung Bean Validation (`@Valid`, `@NotBlank`, `@Positive`, v.v.) tại DTO và Controller.
   - Thống nhất sử dụng `org.springframework.transaction.annotation.Transactional`.
   - Loại bỏ Field Injection `@Autowired` trên thuộc tính; thay thế hoàn toàn bằng Constructor Injection qua Lombok `@RequiredArgsConstructor` với các trường `private final`.
   - Dọn dẹp dead code (`ListToPageUtil.java`) và tạm thời chú thích cấu hình Redis chưa dùng.

---

## 📋 2. BẢNG TỔNG HỢP CÁC FILE ĐÃ CHỈNH SỬA (13 FILES)

| STT | Đường dẫn File | Loại thay đổi | Mô tả tóm tắt |
| :---: | :--- | :---: | :--- |
| 1 | [`book-runner/src/main/java/com/example/bookrunner/repository/BookRepository.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/repository/BookRepository.java) | Repository | Thêm `existsByIsbnAndIdNot`, `existsByCategoryIdIn`, và query bulk soft-delete `@Modifying(clearAutomatically = true, flushAutomatically = true)` |
| 2 | [`book-runner/src/main/java/com/example/bookrunner/service/BookService.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/service/BookService.java) | Service Interface | Bổ sung khai báo phương thức `restoreBook(List<Long> ids)` |
| 3 | [`book-runner/src/main/java/com/example/bookrunner/service/impl/BookServiceImpl.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/service/impl/BookServiceImpl.java) | Service Impl | Fix lỗi ISBN update; chuyển `deleteBook` sang soft-delete; bổ sung `restoreBook`; thêm `@Transactional`; chuyển sang `@RequiredArgsConstructor` |
| 4 | [`book-runner/src/main/java/com/example/bookrunner/controller/BookAPI.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/controller/BookAPI.java) | Controller | Thêm `@Valid` cho `addBook`, `updateBook`; bổ sung endpoint `PATCH /api/v1/books/restore` |
| 5 | [`book-runner/src/main/java/com/example/bookrunner/dto/request/BookRequestDTO.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/dto/request/BookRequestDTO.java) | DTO Request | Thêm các annotation validation: `@NotBlank` (title, author), `@NotNull @Positive` (price), `@PositiveOrZero` (stockQuantity, discountPrice) |
| 6 | [`book-runner/src/main/java/com/example/bookrunner/model/Category.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/model/Category.java) | Entity | Gỡ bỏ `cascade = CascadeType.ALL` ở quan hệ `books` |
| 7 | [`book-runner/src/main/java/com/example/bookrunner/service/impl/CategoryServiceImpl.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/service/impl/CategoryServiceImpl.java) | Service Impl | Kiểm tra chặn xóa danh mục khi đang có sách; chuyển sang Spring `@Transactional` và `@RequiredArgsConstructor` |
| 8 | [`book-runner/src/main/java/com/example/bookrunner/dto/CategoryDTO.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/dto/CategoryDTO.java) | DTO | Bổ sung `@NotBlank(message = "Tên danh mục không được để trống")` cho trường `name` |
| 9 | [`book-runner/src/main/java/com/example/bookrunner/controller/CategoryAPI.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/controller/CategoryAPI.java) | Controller | Bổ sung `@Valid` cho `createCategory` và `updateCategory` |
| 10 | [`book-runner/src/main/java/com/example/bookrunner/service/impl/OrderServiceImpl.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/service/impl/OrderServiceImpl.java) | Service Impl | Cài đặt `TRANSITIONS` Map State Machine, tính lũy đẳng (Idempotent), tự động hoàn kho/doanh số và cập nhật `paymentStatus` |
| 11 | [`book-runner/src/main/java/com/example/bookrunner/service/CartService.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/service/CartService.java) | Service | Chặn thêm sách ngừng kinh doanh (`active = false`) vào giỏ; chuyển sang Spring `@Transactional` và `@RequiredArgsConstructor` |
| 12 | [`book-runner/src/main/java/com/example/bookrunner/exception/GlobalExceptionHandler.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/exception/GlobalExceptionHandler.java) | Exception Handler | Chuẩn hóa 3 handler ngoại lệ (`ItemNotFoundException`, `DuplicateUniqueFieldException`, `FieldRequiredException`) về `ApiResponse<Void>` |
| 13 | [`book-runner/src/main/resources/application.yaml`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/resources/application.yaml) | Config | Tạm thời comment khối cấu hình `spring.data.redis` chưa sử dụng |
| 14 | ~~`book-runner/src/main/java/com/example/bookrunner/util/ListToPageUtil.java`~~ | Dead Code | Đã xóa file tiện ích phân trang in-memory không có nơi nào sử dụng |

---

## 🔍 3. CHI TIẾT THAY ĐỔI TỪNG FILE

### 3.1. `src/main/java/com/example/bookrunner/repository/BookRepository.java`
* **Mục đích:** Bổ sung các phương thức truy vấn hỗ trợ validation ISBN ngoại trừ chính mình, kiểm tra tồn tại sách theo danh mục, và query bulk soft-delete an toàn với L1 Cache.
* **Code thay đổi:**
```java
// Thêm mới:
boolean existsByIsbnAndIdNot(String isbn, Long id);
boolean existsByCategoryIdIn(List<Long> categoryIds);

@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("UPDATE Book b SET b.active = :active WHERE b.id IN :ids")
void updateActiveStatusByIdIn(@Param("ids") List<Long> ids, @Param("active") Boolean active);
```

### 3.2. `src/main/java/com/example/bookrunner/service/impl/BookServiceImpl.java`
* **Mục đích:**
  * Sửa lỗi kiểm tra ISBN khi update: trước đây dùng `findByIsbn()` của chính cuốn sách đang sửa gây lỗi 409 giả tạo.
  * Chuyển `deleteBook` từ hard-delete SQL sang soft-delete (`active = false`), bổ sung hàm `restoreBook` (`active = true`).
  * Bổ sung `@Transactional(rollbackFor = Exception.class)` cho các thao tác ghi dữ liệu.
  * Chuyển từ `@Autowired` sang `@RequiredArgsConstructor`.
* **Code thay đổi tiêu biểu:**
```java
// Trước:
Optional<Book> updating = bookRepository.findById(id);
...
mapper.map(bookRequestDTO, existingBook);
if (bookRepository.findByIsbn(existingBook.getIsbn()).isPresent())
    throw new DuplicateUniqueFieldException("Mã định danh bị trùng!");

// Sau:
Book existingBook = bookRepository.findById(id)
        .orElseThrow(() -> new ItemNotFoundException("Sách không tồn tại với id: " + id));

if (bookRequestDTO.getIsbn() != null && !bookRequestDTO.getIsbn().isBlank()) {
    String cleanIsbn = bookRequestDTO.getIsbn().trim();
    if (bookRepository.existsByIsbnAndIdNot(cleanIsbn, id)) {
        throw new DuplicateUniqueFieldException("Mã định danh (ISBN) '" + cleanIsbn + "' đã được sử dụng bởi cuốn sách khác!");
    }
}
mapper.map(bookRequestDTO, existingBook);
existingBook.setId(id);
```

### 3.3. `src/main/java/com/example/bookrunner/model/Category.java`
* **Mục đích:** Loại bỏ `cascade = CascadeType.ALL` ở danh sách `books` để khi xóa Category không kích hoạt xóa hàng loạt các cuốn sách.
* **Code thay đổi:**
```java
// Trước:
@JsonIgnore
@OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
@Builder.Default
private List<Book> books = new ArrayList<>();

// Sau:
@JsonIgnore
@OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
@Builder.Default
private List<Book> books = new ArrayList<>();
```

### 3.4. `src/main/java/com/example/bookrunner/service/impl/CategoryServiceImpl.java`
* **Mục đích:** Chặn xóa danh mục nếu đang có sách, chuyển sang Spring `@Transactional` và Constructor Injection.
* **Code thay đổi:**
```java
@Transactional(rollbackFor = Exception.class)
@Override
public void deleteByIdIn(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
        throw new BadRequestException("Danh sách ID danh mục cần xóa không được để trống!");
    }
    if (bookRepository.existsByCategoryIdIn(ids)) {
        throw new BadRequestException("Không thể xóa danh mục đang có sách. Vui lòng chuyển sách sang danh mục khác trước khi xóa!");
    }
    categoryRepository.deleteByIdIn(ids);
}
```

### 3.5. `src/main/java/com/example/bookrunner/service/impl/OrderServiceImpl.java`
* **Mục đích:** Cài đặt State Machine khép kín bảo vệ vòng đời đơn hàng, xử lý hoàn kho/doanh số và trạng thái thanh toán.
* **Code thay đổi tiêu biểu:**
```java
private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
        OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
        OrderStatus.CONFIRMED, Set.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED),
        OrderStatus.SHIPPING, Set.of(OrderStatus.DELIVERED, OrderStatus.RETURNED),
        OrderStatus.DELIVERED, Set.of(OrderStatus.RETURNED),
        OrderStatus.CANCELLED, Set.of(),
        OrderStatus.RETURNED, Set.of()
);

@Override
@Transactional(rollbackFor = Exception.class)
public OrderResponseDTO updateOrderStatusForAdmin(Long orderId, OrderStatus newStatus) {
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy đơn hàng với id: " + orderId));

    OrderStatus oldStatus = order.getStatus();

    // 1. Tính lũy đẳng (Idempotent)
    if (newStatus == oldStatus) {
        return mapToDTO(order);
    }

    // 2. Kiểm tra tính hợp lệ qua State Machine
    Set<OrderStatus> allowedTransitions = TRANSITIONS.getOrDefault(oldStatus, Set.of());
    if (!allowedTransitions.contains(newStatus)) {
        throw new BadRequestException("Không thể chuyển đơn hàng từ trạng thái " + oldStatus + " sang " + newStatus);
    }

    // 3. Hoàn lại tồn kho và giảm soldCount nếu CANCELLED hoặc RETURNED
    boolean isReversingStock = (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.RETURNED)
            && (oldStatus != OrderStatus.CANCELLED && oldStatus != OrderStatus.RETURNED);
    if (isReversingStock) {
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

    // 4. Cập nhật trạng thái thanh toán
    if (newStatus == OrderStatus.DELIVERED && order.getPaymentMethod() == PaymentMethod.COD) {
        order.setPaymentStatus(PaymentStatus.PAID);
    } else if ((newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.RETURNED)
            && order.getPaymentStatus() == PaymentStatus.PAID) {
        order.setPaymentStatus(PaymentStatus.REFUNDED);
    }

    order.setStatus(newStatus);
    Order savedOrder = orderRepository.save(order);
    return mapToDTO(savedOrder);
}
```

### 3.6. `src/main/java/com/example/bookrunner/service/CartService.java`
* **Mục đích:** Chặn thêm sách đã ngừng kinh doanh vào giỏ, loại bỏ `@Autowired`, đồng bộ Spring Transactional.
* **Code thay đổi:**
```java
Optional<Book> book = bookRepository.findById(bookId);
if (book.isEmpty())
    throw new ItemNotFoundException("Không tìm thấy sách!");
if (!Boolean.TRUE.equals(book.get().getActive())) {
    throw new BadRequestException("Sách '" + book.get().getTitle() + "' hiện đã ngừng kinh doanh, không thể thêm vào giỏ hàng!");
}
```

### 3.7. `src/main/java/com/example/bookrunner/exception/GlobalExceptionHandler.java`
* **Mục đích:** Đồng bộ 100% phản hồi của các Exception về chuẩn `ResponseEntity<ApiResponse<Void>>`.
* **Code thay đổi:**
```java
// Trước: Trả về Map<String, Object> với key 'error'
public ResponseEntity<Map<String, Object>> handleBookNotFoundException(ItemNotFoundException bnfe) {
    ...
    return new ResponseEntity<>(responseBody, HttpStatus.NOT_FOUND);
}

// Sau: Trả về ApiResponse chuẩn
@ExceptionHandler(ItemNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleBookNotFoundException(ItemNotFoundException bnfe) {
    log.warn("Item not found: {}", bnfe.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(bnfe.getMessage()));
}
```

---

## 🧪 4. KẾT QUẢ KIỂM THỬ XÁC MINH (ĐỢT 1)

1. **Biên dịch mã nguồn:**
   * Lệnh: `./gradlew compileJava`
   * Kết quả: `BUILD SUCCESSFUL in 26s` (0 lỗi, 0 warning cảnh báo syntax).
2. **Kiểm thử tự động các Service:**
   * Lệnh: `./gradlew test --tests "com.example.bookrunner.service.*"`
   * Kết quả: `BUILD SUCCESSFUL in 17s` (Tất cả unit tests đều Passed 100%).

---

## 🚀 5. BỔ SUNG TÍNH NĂNG ĐỒNG BỘ FRONT-END (isFeatured, REVIEW & ADMIN STATS)

Nhằm đồng bộ hoàn toàn với các màn hình của Front-End React (Home, Book Detail, Admin Dashboard), các phân hệ nghiệp vụ sau đã được triển khai hoàn chỉnh:

### 5.1. Bổ sung tham số lọc `isFeatured` cho API Sách
* **Files tác động:**
  * [`BookRepository.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/repository/BookRepository.java): Bổ sung `:isFeatured` vào câu truy vấn và `countQuery` của `searchAndFilterBooks`.
  * [`BookService.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/service/BookService.java) & [`BookServiceImpl.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/service/impl/BookServiceImpl.java): Thêm tham số `Boolean isFeatured` vào phương thức `findAll`.
  * [`BookAPI.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/controller/BookAPI.java): Nhận `@RequestParam(required = false) Boolean isFeatured` tại `GET /api/v1/books`.

### 5.2. Phân hệ Review & Đánh giá Sách
* **DTOs mới:**
  * `CreateReviewRequest`: Validate `rating` (1-5), `comment` (@Size max 1000), `bookId`.
  * `UpdateReviewRequest`: Validate `rating` (1-5), `comment`.
  * `ReviewResponseDTO`: Trả về thông tin đánh giá cùng DTO tóm tắt người dùng (`ReviewUserDTO`).
  * `CanReviewResponseDTO`: Kiểm tra quyền viết đánh giá (`canReview`, `alreadyReviewed`, `hasPurchased`, `reason`).
* **Nghiệp vụ Service & Repository:**
  * [`OrderRepository.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/repository/OrderRepository.java): Bổ sung `hasUserPurchasedBookAndDelivered(userId, bookId)` để xác minh người dùng đã mua sách và đơn hàng ở trạng thái `DELIVERED` mới được phép đánh giá (Admin có quyền bypass).
  * [`ReviewServiceImpl.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/service/impl/ReviewServiceImpl.java):
    * Kiểm tra chặn đánh giá trùng lặp (`existsByUserIdAndBookId`).
    * Tự động tính toán lại `averageRating` và `totalReviews` trên entity `Book` sau mỗi lần thêm/sửa/xóa review.
    * Ghi nhận tương tác `InteractionType.RATING` với trọng số (weight) vào bảng `user_book_interactions` phục vụ thuật toán Recommendation System.
* **Controller & Phân quyền:**
  * [`ReviewController.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/controller/ReviewController.java):
    * `GET /api/v1/books/{bookId}/reviews`: Lấy danh sách đánh giá phân trang (Public).
    * `GET /api/v1/books/{bookId}/can-review`: Kiểm tra điều kiện đánh giá (Yêu cầu đăng nhập).
    * `POST /api/v1/books/{bookId}/reviews`: Tạo đánh giá mới (Yêu cầu đăng nhập).
    * `PUT /api/v1/reviews/{id}`: Sửa đánh giá của chính mình.
    * `DELETE /api/v1/reviews/{id}`: Xóa đánh giá (Chủ sở hữu hoặc Admin).
  * [`SecurityConfig.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/config/SecurityConfig.java): Mở quyền xác thực cho các route đánh giá tương ứng.

### 5.3. Phân hệ Thống kê Quản trị (Admin Dashboard Stats)
* **DTOs mới:**
  * `DashboardStatsResponseDTO`: `totalRevenue`, `totalOrders`, `totalUsers`, `totalBooks`.
  * `RevenueChartDTO`: `month` ("T1".."T12"), `revenue` (BigDecimal).
  * `BestSellerStatDTO`: `title` (String), `sold` (Integer).
* **Truy vấn Aggregate:**
  * [`OrderRepository.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/repository/OrderRepository.java):
    * `sumTotalRevenue()`: Tính tổng doanh thu từ các đơn hàng `DELIVERED`.
    * `getMonthlyRevenueByYear(year)`: Nhóm doanh thu các đơn hàng `DELIVERED` theo tháng.
  * [`BookRepository.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/repository/BookRepository.java):
    * `countByActiveTrue()`: Đếm tổng đầu sách đang mở bán.
    * `findTopBestSellers(pageable)`: Lấy danh sách sách bán chạy nhất theo `soldCount`.
* **Service & Controller:**
  * [`AdminStatsServiceImpl.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/service/impl/AdminStatsServiceImpl.java): Tự động điền đủ 12 tháng (T1 đến T12) ngay cả khi có tháng doanh thu bằng 0 để Front-End vẽ biểu đồ Recharts mượt mà.
  * [`AdminStatsController.java`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/book-runner/src/main/java/com/example/bookrunner/controller/AdminStatsController.java):
    * `GET /api/v1/admin/stats`: Thống kê tổng quan.
    * `GET /api/v1/admin/stats/revenue`: Biểu đồ doanh thu 12 tháng.
    * `GET /api/v1/admin/stats/best-sellers`: Top sách bán chạy nhất (mặc định limit = 10).

### 5.4. Kết quả kiểm thử tự động toàn diện
* **Biên dịch:** `./gradlew compileJava` $\rightarrow$ `BUILD SUCCESSFUL` (0 lỗi).
* **Unit Tests:** `./gradlew test --tests "com.example.bookrunner.service.*"` $\rightarrow$ **100% Passed (41/41 tests thành công)**, bao gồm `ReviewServiceTest` và `AdminStatsServiceTest`.

