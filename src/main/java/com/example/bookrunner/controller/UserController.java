package com.example.bookrunner.controller;

import com.example.bookrunner.dto.auth.UserSummaryResponse;
import com.example.bookrunner.dto.common.ApiResponse;
import com.example.bookrunner.dto.request.ChangePasswordRequest;
import com.example.bookrunner.dto.request.UpdateProfileRequest;
import com.example.bookrunner.security.CustomUserDetails;
import com.example.bookrunner.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UserSummaryResponse response = userService.getProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserSummaryResponse response = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", response));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
    }
}
