# KẾ HOẠCH & DANH MỤC CÔNG VIỆC DỰ ÁN (PROJECT ROADMAP & TASKS)
> **Dự án**: Website Bán Sách Tích Hợp Hệ Thống Gợi Ý (Book-Runner E-Commerce & RecSys)  
> *Thời gian dự kiến: 10 Tuần | Công nghệ: Spring Boot, Python FastAPI, MySQL, Redis, React*  
> *Cập nhật ngày: 29/09/2026*

---

## 📊 TIẾN ĐỘ TỔNG QUAN

```
[Giai đoạn 1: Khảo sát & Data Modeling] ──────────► [100% HOÀN THÀNH]
[Giai đoạn 2: Backend Core (E-Commerce)] ──────────► [ 85% HOÀN THÀNH] (Auth, Catalog, Order, User, Admin: 100%)
[Giai đoạn 3: Recommender System (ML/FastAPI)] ────► [ 20% HOÀN THÀNH] (Schema DB & BUY Tracking sẵn sàng)
[Giai đoạn 4: Frontend Web UI/UX] ─────────────────► [ 90% HOÀN THÀNH] (13 Trang UI, Interceptor, DTO Sync)
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

### 1.4. Phân Hệ Đơn Hàng (Order Subsystem) - 100%
- [x] Tạo đơn hàng mới (`POST /orders`): Hỗ trợ mua ngay hoặc checkout từ giỏ hàng.
- [x] **Trừ tồn kho an toàn (Concurrency Control):** Khóa bi quan `findByIdWithLock` (`PESSIMISTIC_WRITE`) ngăn chặn bán quá số lượng (Overselling).
- [x] Tự động sinh mã đơn hàng duy nhất `ORD-...`.
- [x] Tự động dọn dẹp các sản phẩm đã mua khỏi giỏ hàng `Cart`.
- [x] **Tự động cập nhật số lượng đã bán (`soldCount`):** Tự động tăng `soldCount` khi đặt hàng thành công (`OrderServiceImpl`) và tự động hoàn giảm lại `soldCount` khi khách hàng hoặc admin hủy đơn hàng.
- [x] **Tích hợp RecSys ngầm:** Tự động ghi nhận interaction `BUY` (trọng số 5.0) vào bảng `user_book_interactions`.
- [x] Xem lịch sử đơn hàng của tôi (`GET /orders/my-orders`).
- [x] Xem chi tiết đơn hàng (`GET /orders/{id}`) có kiểm tra quyền sở hữu.
- [x] Khách hàng hủy đơn hàng (`PUT /orders/{id}/cancel`): Chỉ cho phép hủy khi PENDING/CONFIRMED, **tự động hoàn lại tồn kho sách**.
- [x] Quản trị Admin: Xem toàn bộ đơn hàng sàn (`GET /admin/orders`), cập nhật trạng thái đơn (`PUT /admin/orders/{id}/status`), tự hoàn kho nếu hủy, tự cập nhật `PAID` nếu giao thành công đơn COD.
- [x] Bộ Unit Tests `OrderServiceTest` (100% Passed).

### 1.5. Phân Hệ Người Dùng & Hồ Sơ (User Subsystem) - 100%
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

### 1.6. Xử Lý Ngoại Lệ & Chuẩn Hóa API Toàn Cục (Global Exception & Response Format) - 100%
- [x] Chuẩn hóa định dạng phản hồi API toàn hệ thống qua `ApiResponse<T>` (`success`, `message`, `data`, `errors`, `timestamp`).
- [x] Tối ưu `GlobalExceptionHandler`: Bóc tách thông báo lỗi validation đầu tiên từ `MethodArgumentNotValidException` (`BindingResult`) đưa lên thuộc tính `message` cấp cao nhất, giúp giao diện Frontend hiển thị Toast thông báo lỗi trực tiếp và tự nhiên cho người dùng.

### 1.7. Tích Hợp & Đồng Bộ Frontend Web (Frontend Web Integration) - 90%
- [x] Xây dựng hoàn chỉnh giao diện 13 trang người dùng & quản trị bằng React + CSS Module / Global Styles.
- [x] **Chuẩn hóa Axios Interceptor:** Thiết lập `BASE_URL = '/api/v1'`, tách riêng instance `refreshClient` không gắn interceptor response để thực hiện refresh token, loại bỏ hoàn toàn nguy cơ treo trình duyệt do đệ quy khi refresh token hết hạn.
- [x] **Bảo vệ Chống Gian Lận Giá (Anti-Price Tampering):** Client chỉ gửi danh sách `{ bookId, quantity }` khi checkout, giá sách do Backend khóa bi quan (`PESSIMISTIC_WRITE`) và đọc trực tiếp từ DB.
- [x] **Đồng bộ Quy tắc Mật khẩu:** Ràng buộc mật khẩu phía Frontend ($\ge 8$ ký tự, chữ và số) khớp hoàn toàn với Spring Boot validation.
- [x] **Ánh xạ Sắp xếp Sách:** Map tùy chọn "Bán chạy" (`best_seller`) sang `sortBy=soldCount&sortDir=desc`.
- [x] **Thu hẹp Mock Fallback:** Giữ nguyên và hiển thị lỗi thực tế từ Backend (400, 401, 403, 500) qua Toast UI; mock fallback chỉ kích hoạt khi mất kết nối mạng hoặc lỗi 502 Bad Gateway khi Backend chưa khởi động.
- [x] Nhật ký chi tiết 12 file code Frontend đã thay đổi được lưu tại [`FRONTEND_CHANGES.md`](file:///C:/Users/testu/OneDrive/Máy tính/New folder (2)/FRONTEND_CHANGES.md).

---

## 🚀 PHẦN 2: NHỮNG CÔNG VIỆC CẦN THỰC HIỆN TIẾP THEO (TODO TASKS)

### 📌 Giai Đoạn 1: Hoàn Thiện Các API E-Commerce Cốt Lõi Còn Lại

#### Task 1: Phân Hệ Giỏ Hàng (Cart Module)
- [ ] Tạo các DTOs: `AddToCartRequest`, `UpdateCartItemRequest`, `CartResponseDTO`, `CartItemResponseDTO`.
- [ ] Viết `CartService` & `CartServiceImpl`:
  - [ ] `getCart(Long userId)`: Lấy thông tin giỏ hàng của user (tính lại đơn giá mới nhất của sách).
  - [ ] `addToCart(Long userId, AddToCartRequest request)`: Thêm sách vào giỏ (nếu sách đã có thì cộng dồn số lượng, kiểm tra tồn kho tối đa).
  - [ ] `updateCartItemQuantity(Long userId, Long itemId, int quantity)`: Đổi số lượng.
  - [ ] `removeCartItem(Long userId, Long itemId)`: Xóa 1 cuốn sách.
  - [ ] `clearCart(Long userId)`: Xóa sạch giỏ.
- [ ] Viết `CartController` (`/api/v1/cart`).
- [ ] Viết Unit Test cho `CartService`.

#### Task 2: Phân Hệ Đánh Giá & Bình Luận (Review Module)
- [ ] Tạo các DTOs: `CreateReviewRequest`, `ReviewResponseDTO`.
- [ ] Viết `ReviewService` & `ReviewServiceImpl`:
  - [ ] `createReview(Long userId, Long bookId, CreateReviewRequest request)`: Người dùng đánh giá 1-5 sao kèm nhận xét.
  - [ ] Ràng buộc: Mỗi user chỉ được đánh giá 1 lần trên mỗi cuốn sách.
  - [ ] Kiểm tra điều kiện: User phải có đơn hàng đã giao thành công (`DELIVERED`) chứa cuốn sách đó mới được review.
  - [ ] Tự động tính toán lại `averageRating` và `totalReviews` trong bảng `books`.
  - [ ] Tự động ghi nhận interaction `RATING` vào bảng `user_book_interactions`.
  - [ ] `getReviewsByBook(Long bookId, Pageable pageable)`: Lấy danh sách review của sách.
- [ ] Viết `ReviewController` (`/api/v1/books/{bookId}/reviews`).

#### Task 3: Tích Hợp Cổng Thanh Toán VNPay Sandbox
- [ ] Tạo file cấu hình `VNPayConfig.java` (chứa `vnp_TmnCode`, `vnp_HashSecret`, `vnp_Url`, `vnp_ReturnUrl`).
- [ ] Xây dựng tiện ích tính toán mã băm an toàn HMAC-SHA512.
- [ ] Tạo API `POST /api/v1/payments/vnpay/create-url`: Nhận `orderId`, sinh URL redirect sang cổng VNPay Sandbox.
- [ ] Xây dựng endpoint `GET /api/v1/payments/vnpay/callback`: Nhận kết quả người dùng thanh toán xong redirect về giao diện Web.
- [ ] Xây dựng webhook `GET /api/v1/payments/vnpay/ipn`: Nhận xác thực giao dịch ngầm từ server VNPay, kiểm tra chữ ký checksum và cập nhật `paymentStatus = PAID` cho đơn hàng.

---

### 📌 Giai Đoạn 2: Xây Dựng Bộ Thu Thập Dữ Liệu & Pipeline RecSys

#### Task 4: Bộ Thu Thập Hành Vi Người Dùng (Interaction Tracker)
- [x] Tự động ghi nhận tương tác `BUY` khi đơn hàng thành công (đã hoàn thành trong `OrderServiceImpl`).
- [ ] Tạo API `POST /api/v1/interactions/log`:
  - Nhận `bookId` và loại hành vi (`VIEW`, `ADD_TO_CART`).
  - Sử dụng `@Async` của Spring để ghi vào bảng `user_book_interactions` không làm chậm response của người dùng.
- [ ] Chuẩn bị kịch bản sinh dữ liệu mẫu (**Synthetic Data Seeder**):
  - Viết script Python hoặc SQL nạp tập dữ liệu giả lập (50 users mẫu, 200 cuốn sách, khoảng 3.000 - 10.000 tương tác `VIEW`, `ADD_TO_CART`, `BUY`, `RATING`).
  - *Mục đích:* Tránh bẫy thiếu dữ liệu khi bắt đầu huấn luyện thuật toán gợi ý.

#### Task 5: Xây Dựng Python RecSys Microservice (FastAPI)
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

#### Task 6: Xây Dựng Serving API & Fallback Trong Spring Boot
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

### 📌 Giai Đoạn 4: Hoàn Thiện Tích Hợp Frontend Web (Frontend Finalization)

#### Task 7: Đấu Nối API Thật Còn Lại Cho Frontend Khách Hàng
- [x] Đã hoàn thành toàn bộ khung giao diện, routing, state quản lý và tích hợp Auth, Books, Catalog, Orders, Profile.
- [ ] Đấu nối API Giỏ hàng thực tế (`/api/v1/cart`) thay cho Local Storage / State sau khi hoàn thành Task 1.
- [ ] Đấu nối API Đánh giá & Bình luận thực tế (`/api/v1/books/{id}/reviews`) sau khi hoàn thành Task 2.
- [ ] Đấu nối URL redirect thanh toán VNPay Sandbox tại `CheckoutPage.jsx` sau khi hoàn thành Task 3.
- [ ] Bắn sự kiện ngầm gọi API Interaction Tracker (`VIEW`, `ADD_TO_CART`) sau khi hoàn thành Task 4.
- [ ] Đấu nối API Gợi ý sách cá nhân hóa (`/api/v1/recommendations`) vào Carousel trang chủ và trang chi tiết sách sau khi hoàn thành Task 6.

#### Task 8: Hoàn Thiện Frontend Quản Trị (Admin Dashboard)
- [x] Đã hoàn thành giao diện `DashboardPage`, `ManageBooksPage`, `ManageOrdersPage`, `ManageUsersPage`.
- [x] Đã tích hợp API Quản lý Sách (CRUD, Featured), Quản lý Đơn hàng (cập nhật trạng thái), Quản lý Người dùng (phân quyền, khóa/mở khóa).
- [ ] Đấu nối API Thống kê doanh thu & biểu đồ thực tế khi hoàn thiện module Báo cáo.
- [ ] Thêm nút bấm quản trị: "Kích hoạt huấn luyện lại mô hình RecSys" thủ công gọi API FastAPI.

---

### 📌 Giai Đoạn 5: Đánh Giá RecSys, DevOps & Báo Cáo Nghiệm Thu

#### Task 9: Đánh Giá Mô Hình Khuyến Nghị & Tối Ưu
- [ ] Đo lường độ chính xác mô hình RecSys Offline trên tập Test:
  - **Precision@K** & **Recall@K** ($K = 5, 10$).
  - **NDCG (Normalized Discounted Cumulative Gain)**.
  - **Catalog Coverage** (Độ phủ của danh mục sách được gợi ý).
- [ ] Ghi lại kết quả số liệu để đưa vào biểu đồ trong Báo cáo.

#### Task 10: Đóng Gói Hệ Thống & Triển Khai (DevOps)
- [ ] Viết `Dockerfile` cho Backend Spring Boot.
- [ ] Viết `Dockerfile` cho Python FastAPI Service.
- [ ] Viết `docker-compose.yml` khởi chạy đồng bộ:
  - Service 1: `mysql` (Port 3306)
  - Service 2: `redis` (Port 6379)
  - Service 3: `spring-boot-app` (Port 8080)
  - Service 4: `fastapi-recsys` (Port 8000)
- [ ] Chạy kiểm thử tải (Load Testing) qua JMeter hoặc k6.
- [ ] Hoàn thiện Slide thuyết trình và Báo cáo nghiệm thu đồ án.
