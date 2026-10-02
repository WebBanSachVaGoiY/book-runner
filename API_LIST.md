# DANH MỤC RESTFUL APIS - DỰ ÁN BOOK-RUNNER
> **Hệ thống E-Commerce Bán Sách Tích Hợp Recommender System**  
> *Phiên bản: 1.2 | Ngày cập nhật: 30/09/2026*  
> *Trạng thái: 23 API Đã Hoàn Thành | 11 API Cần Thực Hiện Tiếp*

---

## 📌 BẢNG TỔNG HỢP MÃ LỖI VÀ QUY ƯỚC CHUNG

- **Base URL:** `http://localhost:8080/api/v1`
- **Cấu trúc Response Chuẩn:**
  ```json
  {
    "success": true,
    "message": "Thông báo kết quả thao tác",
    "data": {  },
    "timestamp": "2026-09-25T15:30:00"
  }
  ```
- **Xác thực:** Header `Authorization: Bearer <accessToken>`
- **Phân quyền người dùng:**
  - `PUBLIC`: Khách vãng lai, không cần token.
  - `AUTHENTICATED` / `CUSTOMER`: Người dùng đã đăng nhập (`ROLE_CUSTOMER` hoặc `ROLE_ADMIN`).
  - `ADMIN`: Chỉ tài khoản quản trị viên (`ROLE_ADMIN`).
- **Nguyên tắc Bảo mật Giá & Đơn hàng:** Client chỉ gửi `bookId` và `quantity`. Giá bán được Backend khóa bi quan (`PESSIMISTIC_WRITE`) và đọc trực tiếp từ Database, tuyệt đối chống gian lận giá (Price Tampering).
- **Nguyên tắc Sắp xếp Danh mục (Sorting Whitelist):** Hỗ trợ `price`, `averageRating`, `totalReviews`, `createdAt`, `soldCount`. Mọi giá trị khác tự động fallback về `createdAt DESC` (tránh lỗi 500).

---

## 1. PHÂN HỆ XÁC THỰC & NGƯỜI DÙNG (`/api/v1/auth`, `/api/v1/users`)

| STT | Method | Endpoint URI | Quyền hạn | Mô tả chức năng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :---: |
| 1 | `POST` | `/api/v1/auth/register` | `PUBLIC` | Đăng ký tài khoản khách hàng mới, tự động kích hoạt và tạo giỏ hàng rỗng | **[ĐÃ HOÀN THÀNH]** |
| 2 | `POST` | `/api/v1/auth/login` | `PUBLIC` | Đăng nhập hệ thống (username/email), cấp cặp Access & Refresh Token | **[ĐÃ HOÀN THÀNH]** |
| 3 | `POST` | `/api/v1/auth/refresh` | `PUBLIC` | Cấp mới Access Token bằng Refresh Token (Token Rotation, chống Replay) | **[ĐÃ HOÀN THÀNH]** |
| 4 | `POST` | `/api/v1/auth/logout` | `AUTHENTICATED` | Đăng xuất an toàn, tăng `tokenVersion` để thu hồi toàn bộ token ngay lập tức | **[ĐÃ HOÀN THÀNH]** |
| 5 | `GET` | `/api/v1/auth/me` | `AUTHENTICATED` | Xem thông tin hồ sơ của tài khoản đang đăng nhập | **[ĐÃ HOÀN THÀNH]** |
| 6 | `PUT` | `/api/v1/users/profile` | `AUTHENTICATED` | Cập nhật thông tin cá nhân (họ tên, SĐT, địa chỉ giao hàng) | **[ĐÃ HOÀN THÀNH]** |
| 7 | `PUT` | `/api/v1/users/change-password` | `AUTHENTICATED` | Đổi mật khẩu tài khoản và thu hồi các phiên đăng nhập cũ | **[ĐÃ HOÀN THÀNH]** |

---

## 2. PHÂN HỆ DANH MỤC SÁCH (`/api/v1/categories`)

| STT | Method | Endpoint URI | Quyền hạn | Mô tả chức năng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :---: |
| 8 | `GET` | `/api/v1/categories` | `PUBLIC` | Lấy danh sách toàn bộ danh mục sách | **[ĐÃ HOÀN THÀNH]** |
| 9 | `POST` | `/api/v1/categories` | `ADMIN` | Thêm danh mục mới (validate slug độc nhất, chặn Customer) | **[ĐÃ HOÀN THÀNH]** |
| 10 | `PUT` | `/api/v1/categories/{id}` | `ADMIN` | Cập nhật tên, slug, mô tả danh mục | **[ĐÃ HOÀN THÀNH]** |
| 11 | `DELETE` | `/api/v1/categories` | `ADMIN` | Xóa danh mục theo danh sách ID truyền lên | **[ĐÃ HOÀN THÀNH]** |

---

## 3. PHÂN HỆ KHO SÁCH (`/api/v1/books`)

| STT | Method | Endpoint URI | Quyền hạn | Mô tả chức năng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :---: |
| 12 | `GET` | `/api/v1/books` | `PUBLIC` | Tìm kiếm full-text, lọc theo danh mục/khoảng giá, phân trang & sắp xếp | **[ĐÃ HOÀN THÀNH]** |
| 13 | `GET` | `/api/v1/books/{id}` | `PUBLIC` | Xem chi tiết 1 cuốn sách (kèm ID, giá, giảm giá, tồn kho, danh mục) | **[ĐÃ HOÀN THÀNH]** |
| 14 | `POST` | `/api/v1/books` | `ADMIN` | Thêm sách mới vào kho (chặn Customer) | **[ĐÃ HOÀN THÀNH]** |
| 15 | `PUT` | `/api/v1/books/{id}` | `ADMIN` | Cập nhật thông tin chi tiết, giá và số lượng tồn kho sách | **[ĐÃ HOÀN THÀNH]** |
| 16 | `DELETE` | `/api/v1/books` | `ADMIN` | Xóa sách khỏi hệ thống theo danh sách ID | **[ĐÃ HOÀN THÀNH]** |

---

## 4. PHÂN HỆ ĐƠN HÀNG KHÁCH HÀNG (`/api/v1/orders`)

| STT | Method | Endpoint URI | Quyền hạn | Mô tả chức năng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :---: |
| 17 | `POST` | `/api/v1/orders` | `AUTHENTICATED` | Tạo đơn hàng mới (mua ngay hoặc từ giỏ hàng), trừ kho an toàn (Pessimistic Lock), tự động ghi nhận interaction `BUY` | **[ĐÃ HOÀN THÀNH]** |
| 18 | `GET` | `/api/v1/orders/my-orders` | `AUTHENTICATED` | Xem lịch sử đơn hàng của tôi (phân trang, lọc theo trạng thái) | **[ĐÃ HOÀN THÀNH]** |
| 19 | `GET` | `/api/v1/orders/{id}` | `AUTHENTICATED` | Xem chi tiết 1 đơn hàng (kiểm tra quyền sở hữu) | **[ĐÃ HOÀN THÀNH]** |
| 20 | `PUT` | `/api/v1/orders/{id}/cancel` | `AUTHENTICATED` | Khách hàng hủy đơn (chỉ khi PENDING/CONFIRMED), tự động hoàn lại tồn kho | **[ĐÃ HOÀN THÀNH]** |

---

## 5. PHÂN HỆ QUẢN TRỊ ĐƠN HÀNG (`/api/v1/admin/orders`)

| STT | Method | Endpoint URI | Quyền hạn | Mô tả chức năng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :---: |
| 21 | `GET` | `/api/v1/admin/orders` | `ADMIN` | Lấy danh sách toàn bộ đơn hàng sàn (phân trang, lọc theo status) | **[ĐÃ HOÀN THÀNH]** |
| 22 | `PUT` | `/api/v1/admin/orders/{id}/status` | `ADMIN` | Cập nhật trạng thái đơn (CONFIRMED, SHIPPING, DELIVERED, CANCELLED). Tự hoàn kho nếu CANCELLED, tự cập nhật PAID nếu COD | **[ĐÃ HOÀN THÀNH]** |

---

## 6. PHÂN HỆ GIỎ HÀNG (`/api/v1/cart`)

| STT | Method | Endpoint URI | Quyền hạn | Mô tả chức năng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :---: |
| 23 | `GET` | `/api/v1/cart` | `AUTHENTICATED` | Lấy toàn bộ danh sách sản phẩm trong giỏ hàng của user | **[ĐÃ HOÀN THÀNH]** |
| 24 | `POST` | `/api/v1/cart/items` | `AUTHENTICATED` | Thêm sách vào giỏ (`bookId`, `quantity`), kiểm tra tồn kho | **[ĐÃ HOÀN THÀNH]** |
| 25 | `PUT` | `/api/v1/cart/items/{itemId}` | `AUTHENTICATED` | Cập nhật số lượng của một cuốn sách trong giỏ | **[ĐÃ HOÀN THÀNH]** |
| 26 | `DELETE` | `/api/v1/cart/items/{itemId}` | `AUTHENTICATED` | Xóa 1 cuốn sách ra khỏi giỏ hàng | **[ĐÃ HOÀN THÀNH]** |
| 27 | `DELETE` | `/api/v1/cart` | `AUTHENTICATED` | Xóa sạch toàn bộ sản phẩm trong giỏ | **[ĐÃ HOÀN THÀNH]** |

---

## 7. PHÂN HỆ THANH TOÁN VNPAY (`/api/v1/payments`)

| STT | Method | Endpoint URI | Quyền hạn | Mô tả chức năng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :---: |
| 28 | `POST` | `/api/v1/payments/vnpay/create-url` | `AUTHENTICATED` | Tạo URL thanh toán VNPay Sandbox với mã checksum HMAC-SHA512 | *[CẦN LÀM]* |
| 29 | `GET` | `/api/v1/payments/vnpay/callback` | `PUBLIC` | Client redirect về sau khi thanh toán trên cổng VNPay | *[CẦN LÀM]* |
| 30 | `GET` | `/api/v1/payments/vnpay/ipn` | `PUBLIC` | Webhook ngầm từ server VNPay cập nhật trạng thái đơn sang `PAID` | *[CẦN LÀM]* |

---

## 8. PHÂN HỆ ĐÁNH GIÁ & BÌNH LUẬN (`/api/v1/reviews`)

| STT | Method | Endpoint URI | Quyền hạn | Mô tả chức năng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :---: |
| 31 | `GET` | `/api/v1/books/{bookId}/reviews` | `PUBLIC` | Lấy danh sách đánh giá của sách (kèm số sao trung bình & tổng review) | *[CẦN LÀM]* |
| 32 | `POST` | `/api/v1/books/{bookId}/reviews` | `AUTHENTICATED` | Viết đánh giá 1-5 sao, tự tính lại `averageRating` của sách | *[CẦN LÀM]* |
| 33 | `GET` | `/api/v1/books/{bookId}/can-review` | `AUTHENTICATED` | Kiểm tra user đã mua sách này thành công chưa để cho phép review | *[CẦN LÀM]* |

---

## 9. PHÂN HỆ THU THẬP TƯƠNG TÁC (`/api/v1/interactions`) — DÀNH CHO RECSYS

| STT | Method | Endpoint URI | Quyền hạn | Mô tả chức năng | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :---: |
| 34 | `POST` | `/api/v1/interactions/log` | `AUTHENTICATED` | Ghi nhận hành vi người dùng (`VIEW`, `ADD_TO_CART`, `BUY`, `RATING`) chạy bất đồng bộ `@Async` lưu vào `user_book_interactions` | *[CẦN LÀM]* |

---

## 10. PHÂN HỆ GỢI Ý SÁCH (`/api/v1/recommendations`) — TRỌNG TÂM ĐỀ TÀI

| STT | Method | Endpoint URI | Quyền hạn | Mô tả thuật toán / Nghiệp vụ | Trạng thái |
| :---: | :---: | :--- | :---: | :--- | :---: |
| 35 | `GET` | `/api/v1/recommendations/for-you` | `AUTHENTICATED` | **Gợi ý cá nhân hóa ("Dành riêng cho bạn")**:<br>• Nếu $\ge 5$ tương tác: Đọc kết quả Collaborative Filtering ALS từ bảng `user_recommendations`.<br>• Nếu $< 5$ (Cold-start): Fallback sang Sách bán chạy/Đánh giá cao.<br>• Tích hợp Redis Caching (TTL 2-4h). | *[CẦN LÀM]* |
| 36 | `GET` | `/api/v1/recommendations/related/{bookId}` | `PUBLIC` | **Sách tương tự cuốn đang xem**: Đọc từ bảng `book_similarities` dựa trên Content-Based Filtering (Cosine Similarity). | *[CẦN LÀM]* |
| 37 | `GET` | `/api/v1/recommendations/frequently-bought-together/{bookId}` | `PUBLIC` | **Thường được mua cùng**: Khai phá tập phổ biến từ các đơn hàng thực tế (`ALSO_BOUGHT`). | *[CẦN LÀM]* |
| 38 | `GET` | `/api/v1/recommendations/trending` | `PUBLIC` | **Sách bán chạy / Thịnh hành**: Top sách bán chạy nhất trong 30 ngày qua (Non-personalized). | *[CẦN LÀM]* |

---

## 11. PHÂN HỆ PYTHON RECSYS MICROSERVICE (NỘI BỘ FASTAPI)

| STT | Method | Endpoint URI | Mô tả chức năng | Trạng thái |
| :---: | :---: | :--- | :--- | :---: |
| 39 | `POST` | `/api/recsys/train/content-based` | Đọc bảng `books`, tính ma trận TF-IDF Cosine Similarity và lưu vào `book_similarities` | *[CẦN LÀM]* |
| 40 | `POST` | `/api/recsys/train/collaborative` | Đọc bảng `user_book_interactions`, huấn luyện mô hình ALS và lưu top gợi ý vào `user_recommendations` | *[CẦN LÀM]* |
| 41 | `GET` | `/api/recsys/health` | Kiểm tra trạng thái sống của dịch vụ Python | *[CẦN LÀM]* |
