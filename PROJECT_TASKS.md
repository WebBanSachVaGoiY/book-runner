# KẾ HOẠCH & DANH MỤC CÔNG VIỆC DỰ ÁN (PROJECT ROADMAP & TASKS)
> **Dự án**: Website Bán Sách Tích Hợp Hệ Thống Gợi Ý (Book-Runner E-Commerce & RecSys)  
> **Thời gian dự kiến: 10 Tuần | Công nghệ: Spring Boot, Python FastAPI, MySQL / MariaDB, Redis, React**  
> *Cập nhật ngày: 02/10/2026*

---

## 📊 TIẾN ĐỘ TỔNG QUAN

```
[Giai đoạn 1: Khảo sát & Data Modeling] ──────────► [100% HOÀN THÀNH]
[Giai đoạn 2: Backend Core (E-Commerce)] ──────────► [ 98% HOÀN THÀNH] (Auth, Catalog, Cart, Order, User, Review, Stats, isFeatured: 100% | Còn lại: VNPay)
[Giai đoạn 3: Recommender System (ML/FastAPI)] ────► [ 25% HOÀN THÀNH] (Schema DB, Interaction BUY & RATING Tracking sẵn sàng)
[Giai đoạn 4: Frontend Web UI/UX] ─────────────────► [ 92% HOÀN THÀNH] (13 Trang UI, Cart Adapter, Auth/Order Sync)
[Giai đoạn 5: Redis Caching & Tối ưu] ─────────────► [ 25% HOÀN THÀNH] (Config Redis)
[Giai đoạn 6: Đánh giá, Load Test & DevOps] ────────► [  0% CHƯA BẮT ĐẦU]
```

---

## ✅ PHẦN 1: NHỮNG CÔNG VIỆC ĐÃ HOÀN THÀNH (COMPLETED)

### 1.1. Tầng Dữ Liệu (Data Modeling & Repositories) - 100%
- [x] Thiết kế đầy đủ 11 JPA Entities: `User`, `Category`, `Book`, `Cart`, `CartItem`, `Order`, `OrderItem`, `Review`, `UserBookInteraction`, `BookSimilarity`, `UserRecommendation`.
- [x] Bổ sung trường `sold_count` và chỉ mục tìm kiếm `@Index(name = "idx_book_sold_count")` vào Entity `Book`.
- [x] Tạo 7 Enums nghiệp vụ: `Role`, `OrderStatus`, `PaymentMethod`, `PaymentStatus`, `InteractionType`, `RecommendationType`.
- [x] Xây dựng 11 JPA Repositories hoàn chỉnh với các custom query tối ưu: tìm kiếm full-text, lọc sách, khóa bi quan, tính tương đồng, cold-start.

### 1.2. Phân Hệ Xác Thực & Phân Quyền (Authentication & Authorization) - 100%
- [x] Đăng ký (`POST /auth/register`), Đăng nhập (`POST /auth/login`), Cấp mới token (`POST /auth/refresh`), Đăng xuất (`POST /auth/logout`), Xem hồ sơ (`GET /auth/me`).
- [x] Cơ chế bảo mật JWT: HMAC-SHA256, Rotation chống Replay Attack (claim `jti`), thu hồi tức thì qua `tokenVersion`.
- [x] Chống User Enumeration tại endpoint đăng nhập.
- [x] Phân quyền đa tầng (Security Filter Chain + `@PreAuthorize` ở Controller).
- [x] Chuẩn hóa lỗi 401 Unauthorized (`JwtAuthenticationEntryPoint`) và 403 Forbidden (`CustomAccessDeniedHandler`).
- [x] Bộ Unit Tests `AuthServiceTest` (100% Passed).

### 1.3. Phân Hệ Danh Mục & Sách (Catalog Subsystem) - 100%
- [x] Lấy danh mục (`GET /categories`), Thêm (`POST`), Sửa (`PUT`), Xóa (`DELETE`) — Phân quyền chặt chẽ chỉ `ROLE_ADMIN`.
- [x] Tìm kiếm full-text, lọc đa tiêu chí (giá, thể loại), phân trang sách (`GET /books`).
- [x] Chi tiết sách: Bổ sung trường `id` trong `BookResponseDTO` và API `GET /books/{id}`.
- [x] Thêm, sửa, xóa sách dành riêng cho Quản trị viên (`ROLE_ADMIN`).
- [x] **Quản trị sách nổi bật:** API `PATCH /admin/books/{id}/featured` bật/tắt trạng thái nổi bật (`isFeatured`) nhanh chóng.
- [x] **Đồng bộ Sắp xếp Bán chạy (`soldCount`):** Bổ sung trường `soldCount` vào Entity `Book`, index tối ưu tìm kiếm; hỗ trợ sắp xếp theo bán chạy (`sortBy=soldCount&sortDir=desc`).
- [x] **Bảo vệ Sort Whitelist:** Whitelist các trường sắp xếp hợp lệ (`price`, `averageRating`, `totalReviews`, `soldCount`, `createdAt`, `title`) trong `BookServiceImpl`, giá trị lạ fallback an toàn về `createdAt`, ngăn chặn triệt để lỗi HTTP 500.
- [x] **Giải quyết triệt để lỗi N+1 Query & LazyInitializationException:**
  - Bổ sung `LEFT JOIN FETCH b.category` trong `findByIdWithCategory` và `searchAndFilterBooks` (kèm `countQuery` riêng biệt không chứa fetch join).
  - Áp dụng `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` cho Entity `Category`.
  - Áp dụng `@Transactional(readOnly = true)` tại tầng Service cho các truy vấn đọc.
- [x] **Triệt tiêu hoàn toàn Entity Exposure ra REST API:**
  - Refactor `BookResponseDTO` thay thế JPA Entity `Category` bằng POJO `CategoryDTO category`.
  - Bổ sung các trường phẳng primitive `categoryId`, `categoryName`, `isFeatured`, `active`, `soldCount` tương thích ngược 100% với giao diện Frontend.
  - Bọc phản hồi của `BookAPI` và `CategoryAPI` trong chuẩn `ApiResponse<T>`.
- [x] Bộ Unit Tests `BookServiceTest` (100% Passed).

### 1.4. Phân Hệ Giỏ Hàng (Cart Subsystem) - 100% Hoàn Thành Mới
- [x] Thiết kế DTOs: `AddToCartRequest`, `UpdateCartItemRequest`, `CartDTO`, `CartItemDTO`.
- [x] Xây dựng `CartService` & `CartServiceImpl`:
  - [x] `getCart(Long userId)`: Lấy giỏ hàng, tự động tính lại đơn giá mới nhất của từng sách, fallback giá gốc khi `discountPrice` null/rỗng.
  - [x] `addToCart(Long userId, AddToCartRequest request)`: Thêm sách vào giỏ (nếu đã có thì cộng dồn số lượng, kiểm tra tồn kho tối đa).
  - [x] `updateCartItemQuantity(Long userId, Long itemId, int quantity)`: Cập nhật số lượng item trong giỏ (hỗ trợ tra cứu theo cả `cartItem.id` hoặc `book.id`).
  - [x] `removeCartItem(Long userId, Long itemId)`: Xóa 1 cuốn sách khỏi giỏ.
  - [x] `deleteCartItem(Long userId, List<Long> bookIds)`: Xóa hàng loạt nhiều cuốn sách theo danh sách bookId.
  - [x] `clearCart(Long userId)`: Xóa sạch toàn bộ giỏ hàng.
- [x] Xây dựng `CartAPI` (`/api/v1/cart`) chuẩn hóa `ApiResponse<T>`, phân quyền người dùng qua `@AuthenticationPrincipal`.
- [x] Bộ Unit Tests `CartServiceTest` (100% Passed).

### 1.5. Phân Hệ Đơn Hàng (Order Subsystem) - 100%
- [x] Tạo đơn hàng mới (`POST /orders`): Hỗ trợ mua trực tiếp danh sách items hoặc checkout tự động từ toàn bộ giỏ hàng `Cart`.
- [x] **Trừ tồn kho an toàn (Concurrency Control):** Khóa bi quan `findByIdWithLock` (`PESSIMISTIC_WRITE`) ngăn chặn bán quá số lượng (Overselling).
- [x] **Tương thích hoàn toàn MariaDB / XAMPP:** Đổi dialect sang `org.hibernate.dialect.MariaDBDialect` trong `application.yaml`, khắc phục dứt điểm lỗi SQL syntax `FOR UPDATE OF <alias>`.
- [x] Tự động sinh mã đơn hàng duy nhất `ORD-...`.
- [x] Tự động dọn dẹp các sản phẩm đã mua khỏi giỏ hàng `Cart`.
- [x] **Tự động cập nhật số lượng đã bán (`soldCount`):** Tự động tăng `soldCount` khi đặt hàng thành công (`OrderServiceImpl`) và tự động hoàn giảm lại `soldCount` khi khách hàng hoặc admin hủy đơn hàng.
- [x] **Tích hợp RecSys ngầm:** Tự động ghi nhận interaction `BUY` (trọng số 5.0) vào bảng `user_book_interactions`.
- [x] Xem lịch sử đơn hàng của tôi (`GET /orders/my-orders`).
- [x] Xem chi tiết đơn hàng (`GET /orders/{id}`) có kiểm tra quyền sở hữu hoặc quyền Quản trị viên (`ROLE_ADMIN`).
- [x] Khách hàng hủy đơn hàng (`PUT /orders/{id}/cancel`): Chỉ cho phép hủy khi PENDING/CONFIRMED, **tự động hoàn lại tồn kho sách**.
- [x] Quản trị Admin: Xem toàn bộ đơn hàng sàn (`GET /admin/orders`), cập nhật trạng thái đơn (`PUT /admin/orders/{id}/status`), tự hoàn kho nếu hủy, tự cập nhật `PAID` nếu giao thành công đơn COD.
- [x] Bộ Unit Tests `OrderServiceTest` (100% Passed).

### 1.6. Phân Hệ Người Dùng & Hồ Sơ (User Subsystem) - 100%
- [x] **Khách hàng:**
  - Lấy thông tin cá nhân (`GET /users/profile`).
  - Cập nhật hồ sơ (`PUT /users/profile`): Họ tên, số điện thoại, địa chỉ giao hàng.
  - Đổi mật khẩu an toàn (`PUT /users/change-password`): Kiểm tra mật khẩu cũ, mã hóa BCrypt, tăng `tokenVersion` để thu hồi tức thì toàn bộ JWT token cũ trên các thiết bị khác.
- [x] **Quản trị viên (Admin User Management):**
  - Xem danh sách người dùng phân trang & tìm kiếm (`GET /admin/users`).
  - Xem chi tiết người dùng (`GET /admin/users/{id}`).
  - Cập nhật quyền hạn/thông tin người dùng (`PUT /admin/users/{id}`).
  - Bật/tắt trạng thái hoạt động tài khoản (`PATCH /admin/users/{id}/toggle-enabled`).
- [x] Bộ Unit Tests `UserServiceTest` (100% Passed).

### 1.7. Dữ Liệu Khởi Tạo Mẫu (Database Seeder) - 100%
- [x] Xây dựng `DataInit.java` tự động chạy khi khởi động ứng dụng:
  - Khởi tạo tài khoản Quản trị: `admin` / `Password123` (`ROLE_ADMIN`).
  - Khởi tạo tài khoản Khách hàng: `customer01` / `Password123` (`ROLE_CUSTOMER`).
  - Khởi tạo các Danh mục sách chuẩn (Công nghệ thông tin, Kinh tế, Văn học...).
  - Khởi tạo danh sách các đầu sách thực tế kèm giá, giá khuyến mãi, tồn kho và lượt bán mẫu.

### 1.8. Xử Lý Ngoại Lệ & Chuẩn Hóa API Toàn Cục - 100%
- [x] Chuẩn hóa định dạng phản hồi API toàn hệ thống qua `ApiResponse<T>` (`success`, `message`, `data`, `errors`, `timestamp`).
- [x] Tối ưu `GlobalExceptionHandler`: Bóc tách thông báo lỗi validation đầu tiên từ `MethodArgumentNotValidException` (`BindingResult`) đưa lên thuộc tính `message` cấp cao nhất.

### 1.9. Đồng Bộ Tích Hợp Frontend Web (Frontend Integration Phase 1) - Hoàn Thành
- [x] **Adapter DTO Giỏ Hàng:** Bổ sung hàm `normalizeCartData` trong `cartApi.js` của Frontend, tự động ánh xạ cấu trúc `cartItemDTOList` từ Spring Boot sang format lồng `items: [{ book: {...} }]` mà UI yêu cầu.
- [x] **API Xóa hàng loạt giỏ hàng:** Bổ sung method `deleteItems(bookIds)` trong `cartApi.js` khớp với `DELETE /api/v1/cart/items`.
- [x] **Gỡ bỏ ghi chú TODO cũ:** Cập nhật `adminApi.js` gỡ bỏ ghi chú cũ về User Management.
- [x] **Tạo kế hoạch công việc Frontend:** Lưu tại [`FRONTEND_TASKS.md`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/Front_End/FRONTEND_TASKS.md).

### 1.10. Sửa Lỗi Nghiệp Vụ, Toàn Vẹn Dữ Liệu & Soft Delete Sách - 100% Hoàn Thành
- [x] Sửa lỗi kiểm tra trùng lặp ISBN khi cập nhật (`existsByIsbnAndIdNot`), không còn bị 409 khi lưu chính mình.
- [x] Gỡ bỏ `CascadeType.ALL` ở quan hệ `Category -> Book`, chặn xóa danh mục khi đang có sách (`existsByCategoryIdIn`).
- [x] Hoàn thiện State Machine khép kín (`TRANSITIONS`) cho Đơn hàng, bảo đảm tính lũy đẳng (Idempotent), tự động hoàn tồn kho/doanh số và đồng bộ trạng thái thanh toán (`PaymentStatus`).
- [x] Chuyển xóa sách sang Bulk Soft-Delete (`active = false`) với `@Modifying(clearAutomatically = true, flushAutomatically = true)`. Bổ sung API `PATCH /api/v1/books/restore` khôi phục sách.
- [x] Đồng bộ kiểm tra cờ `active` trên toàn hệ thống (chặn thêm sách ngừng kinh doanh vào giỏ hàng).
- [x] Chuẩn hóa toàn bộ phản hồi lỗi trong `GlobalExceptionHandler` về chuẩn `ApiResponse<Void>`.

### 1.11. Lọc Sách Nổi Bật (`isFeatured`) - 100% Hoàn Thành
- [x] Thêm `@RequestParam(required = false) Boolean isFeatured` tại `GET /api/v1/books` (`BookAPI.java`).
- [x] Tích hợp `:isFeatured` trong custom query và countQuery của `BookRepository.searchAndFilterBooks`.
- [x] Cập nhật `BookService` và `BookServiceImpl` xử lý bộ lọc `isFeatured`.

### 1.12. Phân Hệ Đánh Giá & Bình Luận Sách (Review Subsystem) - 100% Hoàn Thành
- [x] Tạo đầy đủ DTOs: `CreateReviewRequest`, `UpdateReviewRequest`, `ReviewResponseDTO`, `CanReviewResponseDTO`.
- [x] Xây dựng `ReviewService` & `ReviewServiceImpl`:
  - [x] Kiểm tra điều kiện: chỉ cho phép đánh giá nếu user đã mua sách và đơn hàng ở trạng thái `DELIVERED` (`OrderRepository.hasUserPurchasedBookAndDelivered`).
  - [x] Ràng buộc mỗi user chỉ được đánh giá 1 lần trên mỗi cuốn sách (`existsByUserIdAndBookId`).
  - [x] Tự động tính toán lại `averageRating` và `totalReviews` trên entity `Book`.
  - [x] Tự động ghi nhận tương tác `InteractionType.RATING` (trọng số theo số sao đánh giá) vào `user_book_interactions` phục vụ RecSys.
  - [x] Hỗ trợ sửa đánh giá và xóa đánh giá (phân quyền chủ sở hữu hoặc Admin).
- [x] Xây dựng `ReviewController` (`/api/v1/books/{bookId}/reviews`, `/api/v1/books/{bookId}/can-review`, `/api/v1/reviews/{id}`).
- [x] Cấu hình Security trong `SecurityConfig.java`.
- [x] Bộ Unit Tests `ReviewServiceTest` (100% Passed).

### 1.13. Phân Hệ Báo Cáo & Thống Kê Quản Trị (Admin Dashboard Stats) - 100% Hoàn Thành
- [x] Tạo DTOs: `DashboardStatsResponseDTO`, `RevenueChartDTO`, `BestSellerStatDTO`.
- [x] Bổ sung custom queries:
  - `OrderRepository`: `sumTotalRevenue()` (tổng doanh thu các đơn DELIVERED), `getMonthlyRevenueByYear(year)` (doanh thu theo tháng).
  - `BookRepository`: `countByActiveTrue()` (tổng đầu sách đang bán), `findTopBestSellers(pageable)` (sách bán chạy theo soldCount).
- [x] Xây dựng `AdminStatsService` & `AdminStatsServiceImpl`: Tự động điền đủ 12 tháng (T1 đến T12) cho biểu đồ doanh thu.
- [x] Xây dựng `AdminStatsController` (`GET /api/v1/admin/stats`, `GET /api/v1/admin/stats/revenue`, `GET /api/v1/admin/stats/best-sellers`).
- [x] Bộ Unit Tests `AdminStatsServiceTest` (100% Passed).

---

## 🚀 PHẦN 2: NHỮNG CÔNG VIỆC CẦN THỰC HIỆN TIẾP THEO (TODO TASKS)

### 📌 Giai Đoạn 1: Hoàn Thiện Các API E-Commerce Cốt Lõi Còn Lại

#### Task 1: Bổ Sung Tham Số Lọc Sách Nổi Bật (`isFeatured`) - ✅ [ĐÃ HOÀN THÀNH Ở BACKEND]
- [x] Cập nhật `BookAPI.java`: Thêm `@RequestParam(required = false) Boolean isFeatured` vào endpoint `GET /api/v1/books`.
- [x] Cập nhật `BookServiceImpl.java`: Lọc sách theo `isFeatured` kết hợp với các bộ lọc hiện tại.
- [x] Cập nhật `BookRepository.java`: Bổ sung tham số vào truy vấn lọc và đếm.

#### Task 2: Phân Hệ Đánh Giá & Bình Luận (Review Module) - ✅ [ĐÃ HOÀN THÀNH Ở BACKEND]
- [x] Tạo các DTOs: `CreateReviewRequest`, `UpdateReviewRequest`, `ReviewResponseDTO`, `CanReviewResponseDTO`.
- [x] Viết `ReviewService` & `ReviewServiceImpl`:
  - [x] `createReview(Long userId, Long bookId, CreateReviewRequest request)`: Người dùng đánh giá 1-5 sao kèm nhận xét.
  - [x] Ràng buộc: Mỗi user chỉ được đánh giá 1 lần trên mỗi cuốn sách.
  - [x] Kiểm tra điều kiện: User phải có đơn hàng đã giao thành công (`DELIVERED`) chứa cuốn sách đó mới được review.
  - [x] Tự động tính toán lại `averageRating` và `totalReviews` trong bảng `books`.
  - [x] Tự động ghi nhận interaction `RATING` vào bảng `user_book_interactions`.
  - [x] `getReviewsByBook(Long bookId, Pageable pageable)`: Lấy danh sách review của sách.
- [x] Viết `ReviewController` (`/api/v1/books/{bookId}/reviews`, `/api/v1/books/{bookId}/can-review`, `/api/v1/reviews/{id}`).
- [ ] Chuyển Frontend `reviewApi.js` từ mock fallback sang gọi API thật (sẵn sàng kết nối).

#### Task 3: Phân Hệ Báo Cáo & Thống Kê Dashboard Admin (Stats Module) - ✅ [ĐÃ HOÀN THÀNH Ở BACKEND]
- [x] Viết `AdminStatsController` (`/api/v1/admin/stats/**`):
  - [x] `GET /api/v1/admin/stats`: Tổng doanh thu, tổng số đơn hàng, tổng số người dùng, tổng số đầu sách.
  - [x] `GET /api/v1/admin/stats/revenue`: Doanh thu theo tháng phục vụ biểu đồ LineChart.
  - [x] `GET /api/v1/admin/stats/best-sellers`: Top sách bán chạy nhất phục vụ biểu đồ BarChart.
- [ ] Chuyển Frontend `DashboardPage.jsx` từ mock data sang gọi API thật (sẵn sàng kết nối).

#### Task 4: Tích Hợp Cổng Thanh Toán VNPay Sandbox - ⏳ [CẦN THỰC HIỆN TIẾP THEO]
- [ ] Tạo file cấu hình `VNPayConfig.java` (chứa `vnp_TmnCode`, `vnp_HashSecret`, `vnp_Url`, `vnp_ReturnUrl`).
- [ ] Xây dựng tiện ích tính toán mã băm an toàn HMAC-SHA512.
- [ ] Tạo API `POST /api/v1/payments/vnpay/create-url`: Nhận `orderId`, sinh URL redirect sang cổng VNPay Sandbox.
- [ ] Xây dựng endpoint `GET /api/v1/payments/vnpay/callback`: Nhận kết quả người dùng thanh toán xong redirect về giao diện Web.
- [ ] Xây dựng webhook `GET /api/v1/payments/vnpay/ipn`: Nhận xác thực giao dịch ngầm từ server VNPay, kiểm tra chữ ký checksum và cập nhật `paymentStatus = PAID` cho đơn hàng.

---

### 📌 Giai Đoạn 2: Xây Dựng Bộ Thu Thập Dữ Liệu & Pipeline RecSys

#### Task 5: Bộ Thu Thập Hành Vi Người Dùng (Interaction Tracker)
- [x] Tự động ghi nhận tương tác `BUY` khi đơn hàng thành công (đã hoàn thành trong `OrderServiceImpl`).
- [ ] Tạo API `POST /api/v1/interactions/log`:
  - Nhận `bookId` và loại hành vi (`VIEW`, `ADD_TO_CART`).
  - Sử dụng `@Async` của Spring để ghi vào bảng `user_book_interactions` không làm chậm response của người dùng.
- [ ] Chuẩn bị kịch bản sinh dữ liệu mẫu (**Synthetic Data Seeder**):
  - Viết script Python hoặc SQL nạp tập dữ liệu giả lập (50 users mẫu, 200 cuốn sách, khoảng 3.000 - 10.000 tương tác `VIEW`, `ADD_TO_CART`, `BUY`, `RATING`).

#### Task 6: Xây Dựng Python RecSys Microservice (FastAPI)
- [ ] Khởi tạo dự án Python:
  - Cấu trúc thư mục: `app/main.py`, `app/models/`, `app/algorithms/`, `app/database.py`.
  - Cài đặt thư viện: `fastapi`, `uvicorn`, `sqlalchemy`, `pymysql`, `scikit-learn`, `implicit`, `pandas`, `numpy`.
- [ ] Kết nối trực tiếp vào MySQL database của dự án.
- [ ] Xây dựng thuật toán **Content-Based Filtering**:
  - Ghép trường dữ liệu sách: `title` + `author` + `category` + `description`.
  - Tính ma trận TF-IDF và Cosine Similarity.
  - Trích xuất Top 10 sách tương tự nhất cho từng cuốn sách và ghi vào bảng `book_similarities`.
- [ ] Xây dựng thuật toán **Collaborative Filtering ALS (Alternating Least Squares)**:
  - Xây dựng ma trận tương tác thưa User-Item từ bảng `user_book_interactions` (sử dụng cột `weight`).
  - Huấn luyện mô hình ALS qua thư viện `implicit`.
  - Dự đoán Top 20 sách gợi ý cá nhân hóa cho từng user và ghi vào bảng `user_recommendations` (`recommendation_type = 'FOR_YOU'`).
- [ ] Tạo các API kích hoạt huấn luyện (Batch Endpoints):
  - `POST /api/recsys/train/content-based`
  - `POST /api/recsys/train/collaborative`

---

### 📌 Giai Đoạn 3: Serving Gợi Ý, Caching & Fallback Cold-Start

#### Task 7: Xây Dựng Serving API & Fallback Trong Spring Boot
- [ ] Tạo `RecommendationService`:
  - [ ] `getForYouRecommendations(Long userId)`:
    - Nếu số lượng tương tác của user $\ge 5$: Lấy dữ liệu cá nhân hóa từ `user_recommendations`.
    - Nếu user mới $< 5$ tương tác (Cold-Start): Fallback sang lấy danh sách sách bán chạy (`findBestSellers`) và sách đánh giá cao nhất.
  - [ ] `getRelatedBooks(Long bookId)`: Đọc từ `book_similarities`.
  - [ ] `getFrequentlyBoughtTogether(Long bookId)`: Gọi query `findFrequentlyBoughtTogetherBookIds` từ `OrderItemRepository`.
  - [ ] `getTrendingBooks()`: Top bán chạy 30 ngày theo `soldCount`.
- [ ] Tạo `RecommendationController` (`/api/v1/recommendations`).
- [ ] **Tích hợp Redis Cache:** Sử dụng `@Cacheable(value = "user_recommendations", key = "#userId")` với thời gian sống (TTL) 2-4 giờ.

---

### 📌 Giai Đoạn 4: Đánh Giá RecSys, DevOps & Báo Cáo Nghiệm Thu

#### Task 8: Đánh Giá Mô Hình Khuyến Nghị & Tối Ưu
- [ ] Đo lường độ chính xác mô hình RecSys Offline trên tập Test:
  - **Precision@K** & **Recall@K** ($K = 5, 10$).
  - **NDCG (Normalized Discounted Cumulative Gain)**.
  - **Catalog Coverage** (Độ phủ của danh mục sách được gợi ý).

#### Task 9: Đóng Gói Hệ Thống & Triển Khai (DevOps)
- [ ] Viết `Dockerfile` cho Backend Spring Boot.
- [ ] Viết `Dockerfile` cho Python FastAPI Service.
- [ ] Viết `docker-compose.yml` khởi chạy đồng bộ:
  - Service 1: `mysql` / `mariadb` (Port 3306)
  - Service 2: `redis` (Port 6379)
  - Service 3: `spring-boot-app` (Port 8080)
  - Service 4: `fastapi-recsys` (Port 8000)
- [ ] Chạy kiểm thử tải (Load Testing) qua JMeter hoặc k6.
- [ ] Hoàn thiện Slide thuyết trình và Báo cáo nghiệm thu đồ án.
