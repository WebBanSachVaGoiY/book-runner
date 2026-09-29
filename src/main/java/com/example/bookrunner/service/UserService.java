package com.example.bookrunner.service;

import com.example.bookrunner.dto.auth.UserSummaryResponse;
import com.example.bookrunner.dto.request.AdminUpdateUserRequest;
import com.example.bookrunner.dto.request.ChangePasswordRequest;
import com.example.bookrunner.dto.request.UpdateProfileRequest;
import com.example.bookrunner.enums.Role;
import org.springframework.data.domain.Page;

public interface UserService {
    UserSummaryResponse getProfile(Long userId);
    UserSummaryResponse updateProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);

    // Admin functions
    Page<UserSummaryResponse> getAllUsers(Role role, int page, int size);
    UserSummaryResponse getUserById(Long id);
    UserSummaryResponse updateUser(Long id, AdminUpdateUserRequest request);
    UserSummaryResponse toggleUserEnabled(Long id);
}
