package com.example.bookrunner.controller;

import com.example.bookrunner.dto.auth.UserSummaryResponse;
import com.example.bookrunner.dto.common.ApiResponse;
import com.example.bookrunner.dto.request.AdminUpdateUserRequest;
import com.example.bookrunner.enums.Role;
import com.example.bookrunner.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> getAllUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<UserSummaryResponse> users = userService.getAllUsers(role, page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getUserById(@PathVariable Long id) {
        UserSummaryResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request
    ) {
        UserSummaryResponse user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin người dùng thành công", user));
    }

    @PatchMapping("/{id}/toggle-enabled")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> toggleUserEnabled(@PathVariable Long id) {
        UserSummaryResponse user = userService.toggleUserEnabled(id);
        return ResponseEntity.ok(ApiResponse.success("Thay đổi trạng thái tài khoản thành công", user));
    }
}
