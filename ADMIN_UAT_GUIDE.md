# TÀI LIỆU HƯỚNG DẪN KIỂM THỬ CHẤP NHẬN ADMIN (ADMIN UAT GUIDE)
> **Dự án**: Website Bán Sách Tích Hợp Recommendation System (`book-runner`)  
> *Phiên bản: 1.0 | Ngày lập: 25/09/2026*  
> *Mục tiêu: Nghiệm thu các API Quản trị viên và cơ chế Phân quyền bảo mật (RBAC)*

---

## 📋 MỤC LỤC
1. [Chuẩn Bị Môi Trường & Tài Khoản Admin](#1-chuẩn-bị-môi-trường--tài-khoản-admin)
2. [Ma Trận Các Ca Kiểm Thử (14 Test Cases)](#2-ma-trận-các-ca-kiểm-thử-14-test-cases)
3. [Kịch Bản Kiểm Thử Chi Tiết Từng Ca](#3-kịch-bản-kiểm-thử-chi-tiết-từng-ca)
   - [Nhóm 1: Kiểm Thử Bảo Mật & Phân Quyền (RBAC)](#nhóm-1-kiểm-thử-bảo-mật--phân-quyền-rbac)
   - [Nhóm 2: Quản Lý Danh Mục Sách (`CategoryAPI`)](#nhóm-2-quản-lý-danh-mục-sách-categoryapi)
   - [Nhóm 3: Quản Lý Kho Sách (`BookAPI`)](#nhóm-3-quản-lý-kho-sách-bookapi)
   - [Nhóm 4: Quản Trị Đơn Hàng Sàn (`AdminOrderController`)](#nhóm-4-quản-trị-đơn-hàng-sàn-adminordercontroller)
4. [Script PowerShell Kiểm Thử Tự Động Một Chạm (One-Click Script)](#4-script-powershell-kiểm-thử-tự-động-một-chạm-one-click-script)

---

## 1. CHUẨN BỊ MÔI TRƯỜNG & TÀI KHOẢN ADMIN

### 1.1. Cấu hình Tài khoản Admin
Mặc định hệ thống gán quyền `ROLE_CUSTOMER` khi đăng ký qua API `/register`. Để có tài khoản Admin thử nghiệm:

1. **Đăng ký tài khoản thử nghiệm** (nếu chưa có):
   ```http
   POST http://localhost:8080/api/v1/auth/register
   Content-Type: application/json

   {
     "username": "admin",
     "email": "admin@example.com",
     "password": "Password123",
     "fullName": "Administrator"
   }
   ```
2. **Cập nhật quyền `ROLE_ADMIN` trong CSDL MySQL**:
   Mở **phpMyAdmin** / **MySQL CLI** / **DBeaver** và chạy câu lệnh:
   ```sql
   UPDATE users SET role = 'ROLE_ADMIN' WHERE username = 'admin';
   ```
3. **Đăng ký thêm 1 tài khoản thường (`ROLE_CUSTOMER`)** để test kịch bản chặn quyền:
   - Username: `customer01`
   - Password: `Password123`
   - Email: `customer01@example.com`

### 1.2. Khởi động Ứng dụng
```powershell
.\gradlew.bat bootRun
```
*Đợi ứng dụng khởi động hoàn tất tại cổng `http://localhost:8080`.*

---

## 2. MA TRẬN CÁC CA KIỂM THỬ (14 TEST CASES)

| Mã TC | Phân hệ | Endpoint | Method | Vai trò Test | Kết quả mong đợi |
| :---: | :--- | :--- | :---: | :---: | :---: |
| **TC-01** | RBAC | `/api/v1/admin/orders` | `GET` | Anonymous (No Token) | `401 Unauthorized` |
| **TC-02** | RBAC | `/api/v1/categories` | `POST` | Customer (`ROLE_CUSTOMER`) | `403 Forbidden` |
| **TC-03** | RBAC | `/api/v1/books` | `POST` | Customer (`ROLE_CUSTOMER`) | `403 Forbidden` |
| **TC-04** | Auth | `/api/v1/auth/login` | `POST` | Admin Login | `200 OK` (Cấp Admin Token) |
| **TC-05** | Category | `/api/v1/categories` | `POST` | Admin (`ROLE_ADMIN`) | `200 OK` (Thêm thành công) |
| **TC-06** | Category | `/api/v1/categories/{id}` | `PUT` | Admin (`ROLE_ADMIN`) | `200 OK` (Cập nhật thành công) |
| **TC-07** | Category | `/api/v1/categories` | `DELETE` | Admin (`ROLE_ADMIN`) | `200 OK` (Xóa danh mục) |
| **TC-08** | Book | `/api/v1/books` | `POST` | Admin (`ROLE_ADMIN`) | `201 Created` (Thêm sách mới) |
| **TC-09** | Book | `/api/v1/books/{id}` | `PUT` | Admin (`ROLE_ADMIN`) | `200 OK` (Cập nhật sách) |
| **TC-10** | Book | `/api/v1/books` | `DELETE` | Admin (`ROLE_ADMIN`) | `200 OK` (Xóa sách) |
| **TC-11** | Order | `/api/v1/admin/orders` | `GET` | Admin (`ROLE_ADMIN`) | `200 OK` (Danh sách toàn bộ đơn) |
| **TC-12** | Order | `/api/v1/admin/orders?status=PENDING` | `GET` | Admin (`ROLE_ADMIN`) | `200 OK` (Lọc đơn theo status) |
| **TC-13** | Order | `/api/v1/admin/orders/{id}/status` | `PUT` | Admin (`ROLE_ADMIN`) | `200 OK` (Đổi sang `SHIPPING`) |
| **TC-14** | Order | `/api/v1/admin/orders/{id}/status` | `PUT` | Admin (`ROLE_ADMIN`) | `200 OK` (Đổi `DELIVERED` $\rightarrow$ COD auto `PAID`) |

---

## 3. KỊCH BẢN KIỂM THỬ CHI TIẾT TỪNG CA

### NHÓM 1: KIỂM THỬ BẢO MẬT & PHÂN QUYỀN (RBAC)

#### TC-01: Chặn truy cập khi không có Token (Chưa đăng nhập)
* **Method & URL**: `GET http://localhost:8080/api/v1/admin/orders`
* **Headers**: *(Để trống, không truyền Authorization)*
* **Kết quả mong đợi**:
  - HTTP Status: `401 Unauthorized`
  - Body (JSON):
    ```json
    {
      "success": false,
      "message": "Bạn cần đăng nhập để thực hiện thao tác này."
    }
    ```

#### TC-02: Chặn Customer thêm mới Danh mục sách
* **Method & URL**: `POST http://localhost:8080/api/v1/categories`
* **Headers**: 
  - `Authorization: Bearer <CUSTOMER_ACCESS_TOKEN>`
  - `Content-Type: application/json`
* **Body (JSON)**:
  ```json
  {
    "name": "Danh mục thử nghiệm trái phép",
    "slug": "danh-muc-trai-phep"
  }
  ```
* **Kết quả mong đợi**:
  - HTTP Status: `403 Forbidden`
  - Body (JSON):
    ```json
    {
      "success": false,
      "message": "Bạn không có quyền thực hiện thao tác này."
    }
    ```

#### TC-03: Chặn Customer thêm mới Sách vào kho
* **Method & URL**: `POST http://localhost:8080/api/v1/books`
* **Headers**: `Authorization: Bearer <CUSTOMER_ACCESS_TOKEN>`
* **Body (JSON)**:
  ```json
  {
    "title": "Sách Hack Quyền",
    "author": "Anonymous",
    "price": 100000,
    "stockQuantity": 10
  }
  ```
* **Kết quả mong đợi**:
  - HTTP Status: `403 Forbidden`
  - Body: `"Bạn không có quyền thực hiện thao tác này."`

#### TC-04: Đăng nhập tài khoản Admin lấy Token
* **Method & URL**: `POST http://localhost:8080/api/v1/auth/login`
* **Headers**: `Content-Type: application/json`
* **Body (JSON)**:
  ```json
  {
    "usernameOrEmail": "admin",
    "password": "Password123"
  }
  ```
* **Kết quả mong đợi**:
  - HTTP Status: `200 OK`
  - Nhận `accessToken` (dùng token này làm `<ADMIN_TOKEN>` cho các bước tiếp theo).

---

### NHÓM 2: QUẢN LÝ DANH MỤC SÁCH (`CategoryAPI`)

#### TC-05: Admin thêm mới Danh mục sách
* **Method & URL**: `POST http://localhost:8080/api/v1/categories`
* **Headers**:
  - `Authorization: Bearer <ADMIN_TOKEN>`
  - `Content-Type: application/json`
* **Body (JSON)**:
  ```json
  {
    "name": "Kỹ Năng Sống & Phát Triển Bản Thân",
    "slug": "ky-nang-song-phat-trien-ban-than",
    "description": "Sách rèn luyện tư duy, kỹ năng mềm và tâm lý học"
  }
  ```
* **Kết quả mong đợi**:
  - HTTP Status: `200 OK`
  - Body: `"Thêm danh mục mới thành công!"`

#### TC-06: Admin cập nhật thông tin Danh mục
* **Method & URL**: `PUT http://localhost:8080/api/v1/categories/{categoryId}`
* **Headers**: `Authorization: Bearer <ADMIN_TOKEN>`
* **Body (JSON)**:
  ```json
  {
    "name": "Kỹ Năng Sống & Tâm Lý Học Ứng Dụng",
    "slug": "ky-nang-song-tam-ly-hoc",
    "description": "Mô tả đã được cập nhật bởi Admin"
  }
  ```
* **Kết quả mong đợi**:
  - HTTP Status: `200 OK`
  - Body: `"Cập nhật danh mục thành công!"`

#### TC-07: Admin xóa Danh mục theo danh sách ID
* **Method & URL**: `DELETE http://localhost:8080/api/v1/categories`
* **Headers**: `Authorization: Bearer <ADMIN_TOKEN>`
* **Body (JSON)**: `[99]` *(truyền mảng các ID danh mục cần xóa)*
* **Kết quả mong đợi**:
  - HTTP Status: `200 OK`
  - Body: `"Xoá danh mục thành công!"`

---

### NHÓM 3: QUẢN LÝ KHO SÁCH (`BookAPI`)

#### TC-08: Admin thêm sách mới vào kho
* **Method & URL**: `POST http://localhost:8080/api/v1/books`
* **Headers**:
  - `Authorization: Bearer <ADMIN_TOKEN>`
  - `Content-Type: application/json`
* **Body (JSON)**:
  ```json
  {
    "title": "Clean Code - Nghệ Thuật Viết Mã Sạch",
    "author": "Robert C. Martin",
    "publisher": "NXB Thông Tin & Truyền Thông",
    "publicationYear": 2023,
    "isbn": "978-6048071234",
    "description": "Cẩm nang hướng dẫn viết code sạch, dễ bảo trì cho lập trình viên",
    "price": 280000,
    "discountPrice": 245000,
    "stockQuantity": 50,
    "coverImageUrl": "https://example.com/clean-code.jpg",
    "pageCount": 464,
    "language": "Tiếng Việt",
    "categoryId": 1
  }
  ```
* **Kết quả mong đợi**:
  - HTTP Status: `201 Created`
  - Body: `"Thêm sách mới thành công"`

#### TC-09: Admin cập nhật thông tin và giá/tồn kho sách
* **Method & URL**: `PUT http://localhost:8080/api/v1/books/{bookId}`
* **Headers**: `Authorization: Bearer <ADMIN_TOKEN>`
* **Body (JSON)**:
  ```json
  {
    "title": "Clean Code (Tái Bản Mới Nhất)",
    "author": "Robert C. Martin",
    "price": 300000,
    "discountPrice": 250000,
    "stockQuantity": 100,
    "categoryId": 1
  }
  ```
* **Kết quả mong đợi**:
  - HTTP Status: `200 OK`
  - Body: `"Cập nhật thông tin thành công"`

#### TC-10: Admin xóa sách khỏi kho
* **Method & URL**: `DELETE http://localhost:8080/api/v1/books`
* **Headers**: `Authorization: Bearer <ADMIN_TOKEN>`
* **Body (JSON)**: `[100]` *(mảng các ID sách muốn xóa)*
* **Kết quả mong đợi**:
  - HTTP Status: `200 OK`
  - Body: `"Xóa sách thành công"`

---

### NHÓM 4: QUẢN TRỊ ĐƠN HÀNG SÀN (`AdminOrderController`)

#### TC-11: Admin xem danh sách toàn bộ đơn hàng sàn (Phân trang)
* **Method & URL**: `GET http://localhost:8080/api/v1/admin/orders?page=0&size=10`
* **Headers**: `Authorization: Bearer <ADMIN_TOKEN>`
* **Kết quả mong đợi**:
  - HTTP Status: `200 OK`
  - Trả về danh sách đơn hàng toàn sàn kèm thông tin người mua, chi tiết các cuốn sách và tổng tiền.

#### TC-12: Admin lọc đơn hàng theo trạng thái
* **Method & URL**: `GET http://localhost:8080/api/v1/admin/orders?status=PENDING&page=0&size=10`
* **Headers**: `Authorization: Bearer <ADMIN_TOKEN>`
* **Kết quả mong đợi**:
  - HTTP Status: `200 OK`
  - Chỉ trả về các đơn hàng đang ở trạng thái `PENDING` (Chờ xử lý).

#### TC-13: Admin chuyển trạng thái đơn sang `SHIPPING` (Đang giao hàng)
* **Method & URL**: `PUT http://localhost:8080/api/v1/admin/orders/{orderId}/status`
* **Headers**: 
  - `Authorization: Bearer <ADMIN_TOKEN>`
  - `Content-Type: application/json`
* **Body (JSON)**:
  ```json
  {
    "status": "SHIPPING"
  }
  ```
* **Kết quả mong đợi**:
  - HTTP Status: `200 OK`
  - `data.status` chuyển thành `"SHIPPING"`.

#### TC-14: Admin xác nhận giao thành công (`DELIVERED`) và Tự động thanh toán COD
* **Method & URL**: `PUT http://localhost:8080/api/v1/admin/orders/{orderId}/status`
* **Headers**: `Authorization: Bearer <ADMIN_TOKEN>`
* **Body (JSON)**:
  ```json
  {
    "status": "DELIVERED"
  }
  ```
* **Kết quả mong đợi**:
  - HTTP Status: `200 OK`
  - `data.status`: `"DELIVERED"`
  - `data.paymentStatus`: Tự động cập nhật thành `"PAID"` nếu phương thức thanh toán là COD.

---

## 4. SCRIPT POWERSHELL KIỂM THỬ TỰ ĐỘNG MỘT CHẠM (ONE-CLICK SCRIPT)

Mở PowerShell tại thư mục dự án và chạy đoạn script sau để kiểm thử tự động toàn bộ luồng:

```powershell
$baseUrl = "http://localhost:8080/api/v1"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   BAT DAU KIEM THU TU DONG TOAN BO ADMIN APIS & RBAC      " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. TC-01: Chặn khi không có Token (401)
Write-Host "`n[TC-01] Goi Admin API khong truyen Token..." -NoNewline
try {
    $res = Invoke-RestMethod -Uri "$baseUrl/admin/orders" -Method Get
    Write-Host " [THAT BAI] Khong bi chan!" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Write-Host " [THANH CONG - 401 Unauthorized]" -ForegroundColor Green
    } else {
        Write-Host " [THAT BAI - Ma loi: $($_.Exception.Response.StatusCode)]" -ForegroundColor Red
    }
}

# 2. TC-04: Đăng nhập Admin lấy Token
Write-Host "`n[TC-04] Dang nhap tai khoan Admin lay Token..." -NoNewline
$adminLoginBody = @{
    usernameOrEmail = "admin"
    password        = "Password123"
} | ConvertTo-Json

try {
    $loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $adminLoginBody -ContentType "application/json"
    $adminToken = $loginRes.data.accessToken
    Write-Host " [THANH CONG - Da lay Token Admin]" -ForegroundColor Green
} catch {
    Write-Host " [THAT BAI - Khong the dang nhap admin. Vui long kiem tra CSDL!]" -ForegroundColor Red
    exit
}

$adminHeaders = @{
    "Authorization" = "Bearer $adminToken"
    "Content-Type"  = "application/json"
}

# 3. TC-05: Admin thêm Danh mục mới
Write-Host "`n[TC-05] Admin them Danh muc moi..." -NoNewline
$randomCode = Get-Random -Minimum 1000 -Maximum 9999
$catBody = @{
    name        = "Danh Muc UAT $randomCode"
    slug        = "danh-muc-uat-$randomCode"
    description = "Mo ta danh muc test tu dong"
} | ConvertTo-Json

try {
    $catRes = Invoke-RestMethod -Uri "$baseUrl/categories" -Method Post -Headers $adminHeaders -Body $catBody
    Write-Host " [THANH CONG - $catRes]" -ForegroundColor Green
} catch {
    Write-Host " [THAT BAI - $($_.Exception.Message)]" -ForegroundColor Red
}

# 4. TC-08: Admin thêm Sách mới
Write-Host "`n[TC-08] Admin them Sach moi vao kho..." -NoNewline
$isbnCode = "978-604" + (Get-Random -Minimum 1000000 -Maximum 9999999)
$bookBody = @{
    title         = "Sach Test UAT $randomCode"
    author        = "Tac Gia Test"
    isbn          = $isbnCode
    price         = 150000
    discountPrice = 120000
    stockQuantity = 20
} | ConvertTo-Json

try {
    $bookRes = Invoke-RestMethod -Uri "$baseUrl/books" -Method Post -Headers $adminHeaders -Body $bookBody
    Write-Host " [THANH CONG - $bookRes]" -ForegroundColor Green
} catch {
    Write-Host " [THAT BAI - $($_.Exception.Message)]" -ForegroundColor Red
}

# 5. TC-11: Admin lấy toàn bộ đơn hàng
Write-Host "`n[TC-11] Admin lay danh sach don hang..." -NoNewline
try {
    $ordersRes = Invoke-RestMethod -Uri "$baseUrl/admin/orders?page=0&size=5" -Method Get -Headers $adminHeaders
    Write-Host " [THANH CONG - Tong so don: $($ordersRes.data.totalElements)]" -ForegroundColor Green
} catch {
    Write-Host " [THAT BAI - $($_.Exception.Message)]" -ForegroundColor Red
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "   HOAN TAT KIEM THU UAT ADMIN & PHAN QUYEN                " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
```
