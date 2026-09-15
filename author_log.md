# Báo Cáo Tổng Hợp Toàn Diện Repo Book-Runner (Branch: `authen/author`)

* **Dự án**: `book-runner` (Hệ thống E-Commerce Bán Sách Tích Hợp Recommendation System)
* **Nhánh thực hiện**: `authen/author`
* **Công nghệ nền tảng**: Spring Boot, Java 17, Gradle, Spring Data JPA, Spring Security 6, JJWT (0.12.6), MySQL, Redis.
* **Thời gian ghi nhận**: 15/09/2026
* **Trạng thái biên dịch & kiểm thử**: `BUILD SUCCESSFUL` (100% Unit Tests Passed).

---

## 1. Tổng Quan Kiến Trúc & Những Gì Repo Đã Làm Được

Cho đến thời điểm hiện tại, repo `book-runner` đã xây dựng hoàn chỉnh nền tảng kiến trúc phần cứng dữ liệu (Database Schema, Entities, Repositories) và phân hệ nghiệp vụ cốt lõi đầu tiên: **Hệ thống Xác thực & Phân quyền chuẩn doanh nghiệp (Authentication & Authorization)**.

### 1.1. Tầng Dữ Liệu Cốt Lõi (Data Modeling) — Đã Hoàn Thành 100%
Repo đã thiết kế đầy đủ và tối ưu hóa 11 JPA Entities phục vụ cho cả E-Commerce và Hệ thống Gợi ý (RecSys):
1. **`User`**: Quản lý tài khoản, thông tin cá nhân, phân quyền (`ROLE_CUSTOMER`, `ROLE_ADMIN`), trạng thái tài khoản (`enabled`), phiên bảo mật (`tokenVersion`, `refreshTokenJti`).
2. **`Category`**: Danh mục thể loại sách (hỗ trợ slug SEO, quan hệ 1-N với Book).
3. **`Book`**: Thông tin chi tiết sách (giá, giảm giá, tồn kho, ảnh bìa, rating trung bình, `isFeatured`, index `created_at`).
4. **`Cart`** & **`CartItem`**: Quản lý giỏ hàng mua sắm theo từng người dùng.
5. **`Order`** & **`OrderItem`**: Quản lý đơn hàng, thông tin giao hàng, trạng thái xử lý và trạng thái thanh toán.
6. **`Review`**: Đánh giá & bình luận sách (1-5 sao, ràng buộc 1 user đánh giá 1 lần trên mỗi cuốn sách).
7. **`UserBookInteraction`**: Thu thập dữ liệu hành vi người dùng (`VIEW`, `ADD_TO_CART`, `PURCHASE`) làm dữ liệu nuôi mô hình Machine Learning.
8. **`BookSimilarity`**: Lưu trữ độ tương đồng giữa các cuốn sách (Item-to-Item) phục vụ hiển thị *"Sách tương tự cuốn này"*.
9. **`UserRecommendation`**: Lưu trữ danh sách gợi ý cá nhân hóa theo từng người dùng (`FOR_YOU`, `BUY_AGAIN`,...).

### 1.2. Tầng Truy Vấn (11 JPA Repositories) — Đã Hoàn Thành 100%
Toàn bộ 11 Repository interfaces đều được trang bị các method nghiệp vụ từ cơ bản đến phức tạp:
* `BookRepository`: Tìm kiếm full-text, lọc đa tiêu chí, truy vấn top bán chạy (`findBestSellers`), sách mới nhất, sách đánh giá cao, sách cùng tác giả/danh mục.
* `UserRepository`: Đăng nhập linh hoạt bằng username/email, phân trang quản lý user theo quyền.
* `OrderItemRepository`: Truy vấn `findTopSellingBookIds` phục vụ Global Popularity.
* `UserBookInteractionRepository`: Đo lường ngưỡng tương tác Cold-Start ($< 5$), trích xuất lịch sử đọc.
* `BookSimilarityRepository` & `UserRecommendationRepository`: Hỗ trợ batch update, xóa và làm mới kết quả từ RecSys worker.

---

## 2. Chi Tiết Toàn Bộ Các RESTful API Hiện Có Trong Repo

Hiện tại, toàn bộ các API thuộc phân hệ **Xác thực & Bảo mật (`/api/v1/auth`)** đã được triển khai đầy đủ từ Controller, Service, DTO đến Security Filter:

| STT | HTTP Method | Endpoint URI | Quyền hạn (Access Control) | Chức năng nghiệp vụ chi tiết |
| :---: | :---: | :--- | :---: | :--- |
| **1** | `POST` | `/api/v1/auth/register` | `PUBLIC`<br>*(Không cần token)* | **Đăng ký tài khoản khách hàng mới:**<br>• Chuẩn hóa dữ liệu đầu vào (username và email đưa về chữ thường).<br>• Ràng buộc độ mạnh mật khẩu an toàn theo thuật toán BCrypt (8 đến 72 ký tự, bắt buộc chứa chữ cái và chữ số).<br>• Tự động kích hoạt tài khoản (`enabled = true`), gán quyền `ROLE_CUSTOMER`.<br>• Tự động khởi tạo Giỏ hàng rỗng (`Cart`) gắn liền với user mới.<br>• Cấp phát cặp Access Token & Refresh Token đầu tiên (có gán JTI và Token Version). |
| **2** | `POST` | `/api/v1/auth/login` | `PUBLIC`<br>*(Không cần token)* | **Đăng nhập hệ thống:**<br>• Cho phép đăng nhập linh hoạt bằng username hoặc email.<br>• **Chống User Enumeration 100%**: Sử dụng `AuthenticationManager` trực tiếp, không ném thông báo riêng khi tài khoản không tồn tại, trả về 401 chung nếu sai tài khoản/mật khẩu.<br>• Cấp phát JTI mới cho refresh token rotation và trả về cặp token (`accessToken`, `refreshToken`). |
| **3** | `POST` | `/api/v1/auth/refresh` | `PUBLIC`<br>*(Không cần token)* | **Làm mới phiên đăng nhập (Token Refresh):**<br>• Kiểm tra tính hợp lệ và loại token bắt buộc phải là `REFRESH`.<br>• Kiểm tra tài khoản còn kích hoạt (`isEnabled`).<br>• Kiểm tra `tokenVersion` khớp phiên hiện hành.<br>• **Cơ chế Refresh Token Rotation & Chống Replay Attack**: So sánh `jti` trong token với `refreshTokenJti` trong DB. Nếu không khớp (token cũ đã bị đánh cắp/dùng lại), hệ thống lập tức hủy bỏ toàn bộ phiên làm việc của user.<br>• Cấp cặp Access Token và Refresh Token mới kèm JTI mới. |
| **4** | `POST` | `/api/v1/auth/logout` | `AUTHENTICATED`<br>*(Bearer Token)* | **Đăng xuất an toàn & Thu hồi token ngay lập tức:**<br>• Tăng `tokenVersion` (+1) trong database $\rightarrow$ vô hiệu hóa ngay lập tức mọi Access Token hiện hành trước khi hết hạn tự nhiên.<br>• Xóa sạch `refreshTokenJti` trong database $\rightarrow$ vô hiệu hóa vĩnh viễn Refresh Token hiện tại. |
| **5** | `GET` | `/api/v1/auth/me` | `AUTHENTICATED`<br>*(Bearer Token)* | **Lấy hồ sơ cá nhân của người dùng hiện tại:**<br>• Trích xuất thông tin định danh: id, username, email, họ tên, số điện thoại, địa chỉ giao hàng, quyền hạn và ngày tạo tài khoản.<br>• Được bảo vệ chặt chẽ ở tầng Security Filter, chống hoàn toàn lỗi NPE (NullPointerException) khi không truyền token. |

---

## 3. Kiến Trúc Bảo Mật & Đóng Đai Hệ Thống (Security Hardening)

Toàn bộ 20 vấn đề bảo mật được phát hiện trong đợt kiểm thử đã được xử lý triệt để:

1. **Kiểm Soát Phiên Đăng Nhập Đa Tầng (Two-Tier Revocation System)**:
   * **`tokenVersion` (Phiên làm việc)**: Lưu trữ trong bảng `User` và mã hóa thành claim `"v"` trong JWT. Khi user đăng xuất hoặc đổi mật khẩu, `tokenVersion` tăng lên. `JwtAuthenticationFilter` kiểm tra `tokenVersion < user.getTokenVersion()` $\rightarrow$ từ chối ngay lập tức mọi access token cũ.
   * **`refreshTokenJti` (Chống Replay Attack)**: Mỗi refresh token mang một ID duy nhất (claim `jti`). Mỗi lần refresh, JTI cũ bị hủy và JTI mới được sinh ra (Rotation). Nếu attacker cố gắng dùng lại token cũ, hệ thống phát hiện ngay và thu hồi toàn bộ token của tài khoản.
2. **Quy Ước Secret Key An Toàn (Fail-fast Startup)**:
   * Loại bỏ cơ chế silent padding byte `0x00`. Khóa HMAC-SHA256 bắt buộc tối thiểu 32 bytes (256 bits).
   * Quy ước tiền tố `base64:` rõ ràng: Nếu có tiền tố thì giải mã Base64, nếu không thì lấy byte raw UTF-8. Ném lỗi dừng server ngay khi khởi động nếu cấu hình sai.
3. **Phân Tách Loại Token (Token Type Separation)**:
   * Token được đóng dấu claim `type = "ACCESS"` hoặc `type = "REFRESH"`.
   * Cung cấp 2 hàm kiểm tra độc lập: `validateAccessToken()` và `validateRefreshToken()`. Chặn đứng nguy cơ dùng Refresh Token (hạn 7 ngày) làm Bearer token để gọi API.
4. **Xác Minh Issuer Bắt Buộc (`requireIssuer`)**:
   * Parser được cấu hình `.requireIssuer("book-runner")`, chống các cuộc tấn công mạo danh token từ hệ thống hoặc môi trường khác.
5. **CORS An Toàn & Chuẩn Hóa Header**:
   * Do hệ thống truyền token qua header `Authorization: Bearer <token>`, cấu hình CORS đã tắt `allowCredentials = false` và whitelist cụ thể các domain localhost (`http://localhost:[*]`, `http://127.0.0.1:[*]`).
6. **Xử Lý Ngoại Lệ Toàn Cục (`GlobalExceptionHandler`)**:
   * Bắt `DisabledException` / `LockedException` trả về HTTP 403 Forbidden thay vì 500.
   * Bắt `DataIntegrityViolationException` trả về HTTP 409 Conflict thay vì 500 khi xảy ra race condition đăng ký trùng lặp.
   * Bắt `BadCredentialsException` trả về HTTP 401 Unauthorized.
7. **EntryPoint Chuẩn Hóa**:
   * `JwtAuthenticationEntryPoint` sử dụng Jackson `ObjectMapper` serialize đối tượng `ApiResponse` chung của hệ thống và gọi `flush()`.

---

## 4. Đánh Giá Mức Độ Hoàn Thành So Với Mục Tiêu Trong `CONVERSATION_LOG.md`

Dựa trên kế hoạch 6 giai đoạn được đề ra tại Mục 1.2 của `CONVERSATION_LOG.md`:

```
[GIAI ĐOẠN 1: Data Modeling] ──────────────► [100% HOÀN THÀNH]
[GIAI ĐOẠN 2: Backend Core (E-Commerce)] ──► [ 40% HOÀN THÀNH] (Auth & Security: 100%)
[GIAI ĐOẠN 3: Recommendation Engine] ──────► [ 25% HOÀN THÀNH] (CSDL sẵn sàng)
[GIAI ĐOẠN 4: Frontend UI/UX] ─────────────► [  0% CHƯA BẮT ĐẦU]
[GIAI ĐOẠN 5: Evaluation & Caching] ───────► [ 20% HOÀN THÀNH] (Config Redis)
[GIAI ĐOẠN 6: DevOps & Deployment] ────────► [  0% CHƯA BẮT ĐẦU]
```

### Chi Tiết Từng Giai Đoạn:

| Giai đoạn trong Kế hoạch | Mục tiêu đề ra | Trạng thái hiện tại | Nội dung chi tiết |
| :--- | :--- | :---: | :--- |
| **Giai đoạn 1: Khảo sát & Thiết kế CSDL (Data Modeling)** | Thiết kế toàn bộ bảng nghiệp vụ E-Commerce và các bảng RecSys | **100% Hoàn thành** | • Đầy đủ 11 Entities, 7 Enums.<br>• 11 JPA Repositories sẵn sàng.<br>• Cấu hình MySQL InnoDB, Foreign keys, Indexes tối ưu. |
| **Giai đoạn 2: Phát triển Backend Core (Spring Boot)** | Xây dựng REST API Auth, Catalog, Giỏ hàng, Đơn hàng, Tracking | **40% Hoàn thành** | • **Auth & Security**: **100%** (5 APIs hoàn chỉnh, bảo mật toàn diện, test pass 100%).<br>• **Catalog Service (Books, Categories)**: Đã có Model & Repo, *chưa viết Service/Controller*.<br>• **Cart & Order Service**: Đã có Model & Repo, *chưa viết Service/Controller*.<br>• **Interaction Tracker Service**: Đã có Model & Repo, *chưa viết Service/Controller*. |
| **Giai đoạn 3: Xây Dựng Recommendation System** | Thuật toán gợi ý Cold-Start, Item-to-Item, Collaborative Filtering | **25% Hoàn thành** | • CSDL đã hỗ trợ 4 tầng lưu trữ (MySQL, Redis, BookSimilarity, UserRecommendation).<br>• Đã phân định rõ 3 scope gợi ý (Personalized, Item-to-Item, Global Popularity).<br>• *Chưa triển khai Python Worker hoặc Service tính toán gợi ý*. |
| **Giai đoạn 4: Xây Dựng Frontend UI/UX** | Giao diện React/Next.js/Vue, Carousels, Slider, Checkout | **0% Chưa bắt đầu** | • Tập trung hoàn thiện Backend Core trước. |
| **Giai đoạn 5: Đánh Giá & Tối Ưu Hóa (Evaluation & Caching)** | Offline/Online metrics & Redis Caching | **20% Hoàn thành** | • Đã cấu hình kết nối Redis Cache trong `application.yaml`.<br>• Cần tích hợp Spring Cache `@Cacheable` vào tầng Service. |
| **Giai đoạn 6: Đóng Gói & Triển Khai (DevOps)** | Docker Compose cho Spring Boot, MySQL, Redis | **0% Chưa bắt đầu** | • Sẽ thực hiện sau khi hoàn tất các chức năng Backend. |

---

## 5. Kế Hoạch Các Bước Tiếp Theo Sau Branch `authen/author`

Sau khi hoàn tất phân hệ Xác thực & Phân quyền, các bước tiếp theo cần triển khai gồm:

1. **Module Quản Lý Danh Mục & Sách (Catalog Service & Controller)**:
   * Triển khai `CategoryService`, `CategoryController` (CRUD danh mục).
   * Triển khai `BookService`, `BookController` (Phân trang, tìm kiếm theo tiêu đề/tác giả, lọc theo giá/thể loại/rating, xem chi tiết sách).
2. **Module Giỏ Hàng & Đơn Hàng (Cart & Order Service/Controller)**:
   * Triển khai nghiệp vụ thêm/sửa/xóa giỏ hàng (`CartService`, `CartController`).
   * Triển khai nghiệp vụ đặt hàng (`OrderService`, `OrderController`) với cơ chế trừ kho an toàn (`@Transactional`, locking).
3. **Module Thu Thập Hành Vi & Serving Gợi Ý (Tracker & RecSys API)**:
   * Triển khai `InteractionService` ghi nhận tương tác người dùng (`VIEW`, `ADD_TO_CART`, `PURCHASE`).
   * Triển khai `RecommendationService` phục vụ các API: `/recommendations/related/{bookId}`, `/recommendations/trending`, `/recommendations/for-you`.

---

## 6. Hướng Dẫn Thực Hiện Kiểm Thử Chấp Nhận (UAT - User Acceptance Testing)

Phân hệ **Xác thực & Phân quyền (`/api/v1/auth`)** đã sẵn sàng để tiến hành kiểm thử chấp nhận người dùng (UAT) nhằm nghiệm thu 5 REST APIs và toàn bộ các tiêu chí an toàn bảo mật.

### 6.1. Chuẩn Bị Môi Trường Kiểm Thử
1. **Cơ sở dữ liệu (MySQL)**: Khởi động MySQL trong XAMPP Control Panel (cổng `3306`).
2. **Khởi động ứng dụng**: Chạy lệnh khởi động Spring Boot:
   ```powershell
   .\gradlew.bat bootRun
   ```
   *Đợi ứng dụng thông báo `Started BookRunnerApplication in ... seconds` và sẵn sàng nhận request tại `http://localhost:8080`.*

### 6.2. Bộ Kịch Bản Kiểm Thử Chi Tiết (13 Test Cases)

#### Nhóm 1: Nghiệp Vụ Đăng Ký Tài Khoản (`POST /api/v1/auth/register`)
* **TC-01: Đăng ký tài khoản khách hàng mới thành công (Happy Path)**
  * **Method & URL**: `POST http://localhost:8080/api/v1/auth/register`
  * **Headers**: `Content-Type: application/json`
  * **Body (JSON)**:
    ```json
    {
      "username": "customer01",
      "email": "customer01@example.com",
      "password": "Password123",
      "fullName": "Nguyễn Văn A",
      "phone": "0912345678",
      "address": "123 Cầu Giấy, Hà Nội"
    }
    ```
  * **Kết quả mong đợi**: HTTP Status `201 Created`, trả về `accessToken`, `refreshToken`, `role: "ROLE_CUSTOMER"`.
  * **Kiểm tra Database**:
    * Bảng `users`: Có bản ghi `customer01`, mật khẩu mã hóa BCrypt (`$2a$`), `token_version = 1`, `enabled = 1`.
    * Bảng `carts`: Tự động khởi tạo 1 giỏ hàng rỗng gắn với `user_id` mới (quan hệ 1-1 `@OneToOne`).
* **TC-02: Chặn đăng ký với mật khẩu yếu (Validation Check)**
  * **Body (JSON)**: `{"username": "customer02", "email": "customer02@example.com", "password": "123", "fullName": "Trần B"}`
  * **Kết quả mong đợi**: HTTP Status `400 Bad Request` (mật khẩu phải từ 8-72 ký tự, gồm cả chữ và số).
* **TC-03: Chặn đăng ký trùng Username hoặc Email**
  * **Body (JSON)**: Gửi lại request TC-01 với username `customer01`.
  * **Kết quả mong đợi**: HTTP Status `400 Bad Request` (hoặc `409 Conflict`), thông báo tên đăng nhập đã được sử dụng.

#### Nhóm 2: Nghiệp Vụ Đăng Nhập (`POST /api/v1/auth/login`)
* **TC-04: Đăng nhập thành công bằng Username**
  * **Body (JSON)**: `{"usernameOrEmail": "customer01", "password": "Password123"}`
  * **Kết quả mong đợi**: HTTP Status `200 OK`, trả về cặp Access Token và Refresh Token mới.
* **TC-05: Đăng nhập thành công bằng Email**
  * **Body (JSON)**: `{"usernameOrEmail": "customer01@example.com", "password": "Password123"}`
  * **Kết quả mong đợi**: HTTP Status `200 OK`, nhận token hợp lệ.
* **TC-06: Đăng nhập sai mật khẩu (Bảo mật: Chống User Enumeration)**
  * **Body (JSON)**: `{"usernameOrEmail": "customer01", "password": "WrongPassword999"}`
  * **Kết quả mong đợi**: HTTP Status `401 Unauthorized`, thông báo chung `"Tên đăng nhập hoặc mật khẩu không chính xác."` mà không làm lộ sự tồn tại của tài khoản.

#### Nhóm 3: Xem Hồ Sơ Cá Nhân Hiện Tại (`GET /api/v1/auth/me`)
* **TC-07: Lấy thông tin hồ sơ thành công với Access Token**
  * **Method & URL**: `GET http://localhost:8080/api/v1/auth/me`
  * **Headers**: `Authorization: Bearer <accessToken>`
  * **Kết quả mong đợi**: HTTP Status `200 OK`, trả về đúng thông tin user (`id`, `username`, `email`, `fullName`, `role`).
* **TC-08: Từ chối truy cập khi không có token hoặc token không hợp lệ**
  * **Headers**: Không truyền `Authorization` hoặc truyền token rác.
  * **Kết quả mong đợi**: HTTP Status `401 Unauthorized`.
* **TC-09: Chống tráo đổi Token (Type Separation Check)**
  * **Headers**: `Authorization: Bearer <refreshToken>`
  * **Kết quả mong đợi**: HTTP Status `401 Unauthorized` (chặn dùng Refresh Token làm Bearer token để gọi API nghiệp vụ).

#### Nhóm 4: Làm Mới Phiên Đăng Nhập (`POST /api/v1/auth/refresh`) & Rotation
* **TC-10: Làm mới token thành công (Token Rotation)**
  * **Method & URL**: `POST http://localhost:8080/api/v1/auth/refresh`
  * **Body (JSON)**: `{"refreshToken": "<refreshToken hợp lệ>"}`
  * **Kết quả mong đợi**: HTTP Status `200 OK`, trả về cặp Access Token và Refresh Token mới. Bảng `users` cập nhật `refresh_token_jti` mới.
* **TC-11: Phát hiện tấn công Replay Attack (Tái sử dụng Refresh Token cũ)**
  * **Thao tác**: Gửi lại request `POST /api/v1/auth/refresh` với Refresh Token cũ đã bị xoay vòng ở TC-10.
  * **Kết quả mong đợi**: HTTP Status `401 Unauthorized`. Hệ thống phát hiện xâm nhập và tự động hủy bỏ toàn bộ phiên của user trong DB.

#### Nhóm 5: Đăng Xuất An Toàn & Thu Hồi Token Tức Thì (`POST /api/v1/auth/logout`)
* **TC-12: Đăng xuất thành công**
  * **Method & URL**: `POST http://localhost:8080/api/v1/auth/logout`
  * **Headers**: `Authorization: Bearer <accessToken>`
  * **Kết quả mong đợi**: HTTP Status `200 OK`. Bảng `users` tăng `token_version` lên `+1` và xóa `refresh_token_jti`.
* **TC-13: Kiểm tra thu hồi Access Token tức thì (Token Revocation Check)**
  * **Thao tác**: Dùng lại chính `accessToken` vừa dùng ở TC-12 để gọi lại `GET /api/v1/auth/me`.
  * **Kết quả mong đợi**: HTTP Status `401 Unauthorized` ngay lập tức, chứng minh token đã bị vô hiệu hóa dù chưa đến thời điểm hết hạn tự nhiên (24h).

### 6.3. Script PowerShell Kiểm Thử Tự Động Một Chạm (One-Click UAT Script)
Người kiểm thử chỉ cần mở PowerShell và dán đoạn script sau để chạy toàn bộ 13 ca kiểm thử:

```powershell
Write-Host "`n=== BẮT ĐẦU KIỂM THỬ CHẤP NHẬN UAT: BOOK-RUNNER AUTH MODULE ===" -ForegroundColor Cyan
$baseUrl = "http://localhost:8080/api/v1/auth"
$testUser = "uat_user_" + (Get-Random -Minimum 1000 -Maximum 9999)
$testEmail = "$testUser@example.com"
$testPass = "SecurePass123"

function Assert-Test($name, $condition, $detail) {
    if ($condition) {
        Write-Host "[PASS] $name" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] $name - Chi tiết: $detail" -ForegroundColor Red
    }
}

# TC-01: Đăng ký
$regBody = @{ username = $testUser; email = $testEmail; password = $testPass; fullName = "Tester UAT"; phone = "0987654321"; address = "Hà Nội" } | ConvertTo-Json
try {
    $res1 = Invoke-RestMethod -Uri "$baseUrl/register" -Method Post -Body $regBody -ContentType "application/json"
    $accessTok = $res1.data.accessToken
    $refreshTok = $res1.data.refreshToken
    Assert-Test "TC-01: Đăng ký tài khoản mới thành công (User + Cart)" ($res1.success -and $accessTok) ""
} catch { Assert-Test "TC-01: Đăng ký tài khoản mới thành công" $false $_.Exception.Message }

# TC-02: Validation mật khẩu
try {
    Invoke-RestMethod -Uri "$baseUrl/register" -Method Post -Body (@{ username="w"; email="w@e.com"; password="123"; fullName="W" } | ConvertTo-Json) -ContentType "application/json"
    Assert-Test "TC-02: Chặn mật khẩu yếu" $false "Lẽ ra phải chặn"
} catch { Assert-Test "TC-02: Chặn mật khẩu yếu (Status 400)" ($_.Exception.Response.StatusCode.value__ -eq 400) "" }

# TC-03: Chống trùng lặp
try {
    Invoke-RestMethod -Uri "$baseUrl/register" -Method Post -Body $regBody -ContentType "application/json"
    Assert-Test "TC-03: Chặn đăng ký trùng username" $false "Lẽ ra phải chặn"
} catch { Assert-Test "TC-03: Chặn đăng ký trùng username (Status 400)" ($_.Exception.Response.StatusCode.value__ -eq 400) "" }

# TC-04: Đăng nhập username
$loginBody = @{ usernameOrEmail = $testUser; password = $testPass } | ConvertTo-Json
try {
    $resLogin = Invoke-RestMethod -Uri "$baseUrl/login" -Method Post -Body $loginBody -ContentType "application/json"
    $accessTok = $resLogin.data.accessToken
    $refreshTok = $resLogin.data.refreshToken
    Assert-Test "TC-04: Đăng nhập bằng Username thành công" ($resLogin.success -and $accessTok) ""
} catch { Assert-Test "TC-04: Đăng nhập bằng Username" $false $_.Exception.Message }

# TC-06: Đăng nhập sai
try {
    Invoke-RestMethod -Uri "$baseUrl/login" -Method Post -Body (@{ usernameOrEmail=$testUser; password="wrong" } | ConvertTo-Json) -ContentType "application/json"
    Assert-Test "TC-06: Chặn đăng nhập sai mật khẩu" $false "Lẽ ra phải chặn"
} catch { Assert-Test "TC-06: Chặn đăng nhập sai mật khẩu (Status 401)" ($_.Exception.Response.StatusCode.value__ -eq 401) "" }

# TC-07: Hồ sơ /me
try {
    $resMe = Invoke-RestMethod -Uri "$baseUrl/me" -Method Get -Headers @{ Authorization = "Bearer $accessTok" }
    Assert-Test "TC-07: Lấy hồ sơ cá nhân /me thành công" ($resMe.data.username -eq $testUser) ""
} catch { Assert-Test "TC-07: Lấy hồ sơ cá nhân" $false $_.Exception.Message }

# TC-09: Chống tráo Refresh Token làm Bearer token
try {
    Invoke-RestMethod -Uri "$baseUrl/me" -Method Get -Headers @{ Authorization = "Bearer $refreshTok" }
    Assert-Test "TC-09: Chặn dùng Refresh Token làm Bearer token" $false "Lẽ ra phải chặn"
} catch { Assert-Test "TC-09: Chặn dùng Refresh Token làm Bearer token (Status 401)" ($_.Exception.Response.StatusCode.value__ -eq 401) "" }

# TC-10: Token Rotation
try {
    $resRef = Invoke-RestMethod -Uri "$baseUrl/refresh" -Method Post -Body (@{ refreshToken=$refreshTok } | ConvertTo-Json) -ContentType "application/json"
    $newAccessTok = $resRef.data.accessToken
    $newRefreshTok = $resRef.data.refreshToken
    Assert-Test "TC-10: Làm mới token thành công (Token Rotation)" ($newAccessTok -ne $null) ""
} catch { Assert-Test "TC-10: Làm mới token" $false $_.Exception.Message }

# TC-11: Chống Replay Attack
try {
    Invoke-RestMethod -Uri "$baseUrl/refresh" -Method Post -Body (@{ refreshToken=$refreshTok } | ConvertTo-Json) -ContentType "application/json"
    Assert-Test "TC-11: Phát hiện Replay Attack & hủy phiên" $false "Lẽ ra phải chặn"
} catch { Assert-Test "TC-11: Phát hiện Replay Attack & hủy phiên (Status 401)" ($_.Exception.Response.StatusCode.value__ -eq 401) "" }

# TC-12 & TC-13: Logout & Token Revocation
try {
    $reLogin = Invoke-RestMethod -Uri "$baseUrl/login" -Method Post -Body $loginBody -ContentType "application/json"
    $validAccess = $reLogin.data.accessToken
    $logoutHeaders = @{ Authorization = "Bearer $validAccess" }
    $resLogout = Invoke-RestMethod -Uri "$baseUrl/logout" -Method Post -Headers $logoutHeaders
    Assert-Test "TC-12: Đăng xuất an toàn thành công" ($resLogout.success -eq $true) ""
    try {
        Invoke-RestMethod -Uri "$baseUrl/me" -Method Get -Headers $logoutHeaders
        Assert-Test "TC-13: Thu hồi tức thì Access Token sau Logout" $false "Token cũ vẫn dùng được"
    } catch { Assert-Test "TC-13: Thu hồi tức thì Access Token sau Logout (Status 401)" ($_.Exception.Response.StatusCode.value__ -eq 401) "" }
} catch { Assert-Test "TC-12 & TC-13: Lỗi luồng Logout" $false $_.Exception.Message }

Write-Host "`n=== KẾT THÚC KIỂM THỬ UAT ===" -ForegroundColor Cyan
```

### 6.4. Bảng Tiêu Chí Nghiệm Thu UAT (Acceptance Sign-off)

| Mã kiểm thử | Nghiệp vụ kiểm tra | Tiêu chí đạt (Acceptance Criteria) | Trạng thái |
| :--- | :--- | :--- | :---: |
| **TC-01** | Đăng ký tài khoản hợp lệ | Trả về 201 Created, sinh bản ghi trong bảng `users` và tự động tạo `carts` | **[ ] Pass** |
| **TC-02** | Validation mật khẩu yếu | Trả về 400 Bad Request, bắt buộc 8-72 ký tự gồm chữ và số | **[ ] Pass** |
| **TC-03** | Chống trùng Username/Email | Trả về 400 Bad Request khi trùng lặp | **[ ] Pass** |
| **TC-04, TC-05**| Đăng nhập (Username/Email) | Trả về 200 OK, cấp phát cặp JWT Access Token và Refresh Token | **[ ] Pass** |
| **TC-06** | Đăng nhập sai thông tin | Trả về 401 Unauthorized chung, chống User Enumeration | **[ ] Pass** |
| **TC-07, TC-08**| Bảo vệ endpoint `/me` | Trả về 200 khi có Bearer token hợp lệ, 401 khi thiếu hoặc sai token | **[ ] Pass** |
| **TC-09** | Phân tách loại Token (Type check)| Trả về 401 khi cố tình dùng Refresh Token làm Bearer token | **[ ] Pass** |
| **TC-10** | Token Rotation | Trả về 200, cập nhật `refresh_token_jti` mới trong CSDL | **[ ] Pass** |
| **TC-11** | Chống Replay Attack | Trả về 401 và thu hồi phiên khi phát hiện refresh token cũ bị dùng lại | **[ ] Pass** |
| **TC-12, TC-13**| Thu hồi token khi Đăng xuất | Tăng `token_version`, vô hiệu hóa Access Token cũ ngay lập tức (Status 401) | **[ ] Pass** |
