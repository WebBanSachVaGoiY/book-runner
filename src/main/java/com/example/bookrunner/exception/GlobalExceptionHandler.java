package com.example.bookrunner.exception;

import com.example.bookrunner.dto.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ApiResponse<Void>> handleBadRequestException(BadRequestException ex) {
                log.warn("Bad request: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(UsernameNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
                log.warn("Username not found: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
                log.warn("Bad credentials: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ApiResponse.error("Tên đăng nhập hoặc mật khẩu không chính xác."));
        }

        @ExceptionHandler(DisabledException.class)
        public ResponseEntity<ApiResponse<Void>> handleDisabledException(DisabledException ex) {
                log.warn("Tài khoản bị vô hiệu hóa: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.error(
                                                "Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên."));
        }

        @ExceptionHandler(LockedException.class)
        public ResponseEntity<ApiResponse<Void>> handleLockedException(LockedException ex) {
                log.warn("Tài khoản bị tạm khóa: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.error(
                                                "Tài khoản của bạn đã bị tạm khóa. Vui lòng liên hệ quản trị viên."));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
                log.warn("Access denied: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.error("Bạn không có quyền thực hiện thao tác này."));
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
                        DataIntegrityViolationException ex) {
                log.warn("Xung đột dữ liệu hoặc vi phạm ràng buộc toàn vẹn: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiResponse.error(
                                                "Dữ liệu vi phạm ràng buộc toàn vẹn hoặc đã tồn tại trong hệ thống."));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
                        MethodArgumentNotValidException ex) {
                Map<String, String> errors = new HashMap<>();
                for (FieldError error : ex.getBindingResult().getFieldErrors()) {
                        errors.put(error.getField(), error.getDefaultMessage());
                }
                String errorMessage = ex.getBindingResult().getFieldErrors().isEmpty()
                                ? "Dữ liệu đầu vào không hợp lệ"
                                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.<Map<String, String>>builder()
                                                .success(false)
                                                .message(errorMessage)
                                                .data(errors)
                                                .build());
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex) {
                log.error("Internal server error: ", ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.error("Đã xảy ra lỗi hệ thống, vui lòng thử lại sau."));
        }

        // Xử lý tham số/đường dẫn sai kiểu (ví dụ: ?page=abc hoặc enum không hợp lệ) -> 400 thay vì 500
        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
                        MethodArgumentTypeMismatchException ex) {
                log.warn("Sai kiểu tham số '{}': {}", ex.getName(), ex.getValue());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error("Tham số '" + ex.getName() + "' không hợp lệ: " + ex.getValue()));
        }

        // Xử lý body JSON hỏng hoặc sai kiểu dữ liệu -> 400
        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
                log.warn("Không đọc được request body: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error("Dữ liệu JSON hoặc kiểu dữ liệu gửi lên không hợp lệ."));
        }

        // Xử lý thiếu tham số bắt buộc trên query string -> 400
        @ExceptionHandler(MissingServletRequestParameterException.class)
        public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(
                        MissingServletRequestParameterException ex) {
                log.warn("Thiếu tham số bắt buộc: {}", ex.getParameterName());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error("Thiếu tham số bắt buộc: " + ex.getParameterName()));
        }

        // Xử lý vi phạm ràng buộc @Validated trên tham số phương thức -> 400
        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
                log.warn("Vi phạm ràng buộc dữ liệu: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error("Dữ liệu không hợp lệ: " + ex.getMessage()));
        }

        // Xử lý gọi sai phương thức HTTP -> 405
        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupported(
                        HttpRequestMethodNotSupportedException ex) {
                log.warn("Phương thức HTTP không được hỗ trợ: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                                .body(ApiResponse.error("Phương thức HTTP không được hỗ trợ cho endpoint này."));
        }

        // Exception xử lý field unique bị trùng
        @ExceptionHandler(DuplicateUniqueFieldException.class)
        public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateUniqueFieldException die) {
                log.warn("Duplicate field: {}", die.getMessage());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiResponse.error(die.getMessage()));
        }

        // Exception xử lý dữ liệu không tồn tại
        @ExceptionHandler(ItemNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleItemNotFoundException(ItemNotFoundException bnfe) {
                log.warn("Item not found: {}", bnfe.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(bnfe.getMessage()));
        }

        // Exception xử lý nhập thiếu field
        @ExceptionHandler(FieldRequiredException.class)
        public ResponseEntity<ApiResponse<Void>> handleFieldRequiredException(FieldRequiredException fre) {
                log.warn("Field required: {}", fre.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(fre.getMessage()));
        }

        // Lỗi khi gọi dịch vụ ngoài (RecSys FastAPI) → 502 Bad Gateway thay vì 500
        @ExceptionHandler(RestClientException.class)
        public ResponseEntity<ApiResponse<Void>> handleRestClientException(RestClientException ex) {
                log.error("Lỗi gọi dịch vụ ngoài: ", ex);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                                .body(ApiResponse.error(
                                                "Không kết nối được dịch vụ RecSys. Vui lòng kiểm tra service đã chạy chưa."));
        }
}
