package com.example.bookrunner.service.impl;

import com.example.bookrunner.dto.auth.UserSummaryResponse;
import com.example.bookrunner.dto.request.AdminUpdateUserRequest;
import com.example.bookrunner.dto.request.ChangePasswordRequest;
import com.example.bookrunner.dto.request.UpdateProfileRequest;
import com.example.bookrunner.enums.Role;
import com.example.bookrunner.exception.BadRequestException;
import com.example.bookrunner.exception.ItemNotFoundException;
import com.example.bookrunner.model.User;
import com.example.bookrunner.repository.UserRepository;
import com.example.bookrunner.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy người dùng với id: " + userId));
        return mapToDTO(user);
    }

    @Override
    @Transactional
    public UserSummaryResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy người dùng với id: " + userId));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress().trim());
        }

        User savedUser = userRepository.save(user);
        log.info("Người dùng '{}' đã cập nhật thông tin cá nhân", savedUser.getUsername());
        return mapToDTO(savedUser);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy người dùng với id: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu hiện tại không chính xác");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // Tăng token version và xóa refresh token JTI để thu hồi tất cả phiên đăng nhập cũ
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setRefreshTokenJti(null);

        userRepository.save(user);
        log.info("Người dùng '{}' đã đổi mật khẩu thành công. Các phiên đăng nhập cũ đã bị thu hồi.", user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getAllUsers(Role role, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = (role != null)
                ? userRepository.findByRole(role, pageable)
                : userRepository.findAll(pageable);
        return users.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy người dùng với id: " + id));
        return mapToDTO(user);
    }

    @Override
    @Transactional
    public UserSummaryResponse updateUser(Long id, AdminUpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy người dùng với id: " + id));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress().trim());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
            if (!request.getEnabled()) {
                user.setTokenVersion(user.getTokenVersion() + 1);
                user.setRefreshTokenJti(null);
            }
        }

        User saved = userRepository.save(user);
        log.info("Admin cập nhật người dùng ID {}: username={}", saved.getId(), saved.getUsername());
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public UserSummaryResponse toggleUserEnabled(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Không tìm thấy người dùng với id: " + id));

        boolean newStatus = !Boolean.TRUE.equals(user.getEnabled());
        user.setEnabled(newStatus);
        if (!newStatus) {
            user.setTokenVersion(user.getTokenVersion() + 1);
            user.setRefreshTokenJti(null);
        }

        User saved = userRepository.save(user);
        log.info("Admin chuyển trạng thái tài khoản ID {}: enabled={}", saved.getId(), newStatus);
        return mapToDTO(saved);
    }

    private UserSummaryResponse mapToDTO(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
