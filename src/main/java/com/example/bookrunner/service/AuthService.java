package com.example.bookrunner.service;

import com.example.bookrunner.dto.auth.AuthResponse;
import com.example.bookrunner.dto.auth.LoginRequest;
import com.example.bookrunner.dto.auth.RefreshTokenRequest;
import com.example.bookrunner.dto.auth.RegisterRequest;
import com.example.bookrunner.dto.auth.UserSummaryResponse;
import com.example.bookrunner.enums.Role;
import com.example.bookrunner.exception.BadRequestException;
import com.example.bookrunner.model.Cart;
import com.example.bookrunner.model.User;
import com.example.bookrunner.repository.CartRepository;
import com.example.bookrunner.repository.UserRepository;
import com.example.bookrunner.security.CustomUserDetails;
import com.example.bookrunner.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedUsername = request.getUsername().trim().toLowerCase();
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new BadRequestException("Tên đăng nhập '" + request.getUsername() + "' đã được sử dụng.");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email '" + request.getEmail() + "' đã được đăng ký.");
        }

        String initialJti = UUID.randomUUID().toString();

        User user = User.builder()
                .username(normalizedUsername)
                .password(passwordEncoder.encode(request.getPassword()))
                .email(normalizedEmail)
                .fullName(request.getFullName().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .role(Role.ROLE_CUSTOMER)
                .enabled(true)
                .tokenVersion(1L)
                .refreshTokenJti(initialJti)
                .build();

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Xung đột trùng lặp khi đăng ký tài khoản '{}': {}", normalizedUsername, ex.getMessage());
            throw new BadRequestException("Tên đăng nhập hoặc email đã tồn tại.");
        }

        // Khởi tạo giỏ hàng mặc định cho user mới
        Cart cart = Cart.builder()
                .user(savedUser)
                .build();
        cartRepository.save(cart);

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String accessToken = tokenProvider.generateAccessToken(userDetails);
        String refreshToken = tokenProvider.generateRefreshToken(userDetails, initialJti);

        log.info("Người dùng '{}' đã đăng ký tài khoản thành công với ID {}", savedUser.getUsername(), savedUser.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole().name())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Chuẩn hóa input để tránh lệch case sensitivity giữa login và register
        String loginIdentifier = request.getUsernameOrEmail().trim().toLowerCase();

        // Xác thực trực tiếp qua AuthenticationManager (chống User Enumeration)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginIdentifier, request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        // Tạo JTI mới cho refresh token rotation và lưu vào DB
        String newJti = UUID.randomUUID().toString();
        user.setRefreshTokenJti(newJti);
        userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(userDetails);
        String refreshToken = tokenProvider.generateRefreshToken(userDetails, newJti);

        log.info("Người dùng '{}' đã đăng nhập thành công", user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // 1. Kiểm tra tính hợp lệ và loại token (bắt buộc phải là REFRESH token)
        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            throw new BadRequestException("Refresh token không hợp lệ hoặc đã hết hạn.");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        String tokenJti = tokenProvider.getJtiFromToken(refreshToken);
        Long tokenVersion = tokenProvider.getTokenVersionFromToken(refreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng cho refresh token."));

        if (!user.getEnabled()) {
            throw new BadRequestException("Tài khoản đã bị vô hiệu hóa.");
        }

        // 2. Kiểm tra token version (nếu user đã logout toàn bộ phiên)
        if (tokenVersion == null || !tokenVersion.equals(user.getTokenVersion())) {
            throw new BadRequestException("Phiên đăng nhập đã bị thu hồi. Vui lòng đăng nhập lại.");
        }

        // 3. Kiểm tra JTI chống Replay Attack (Refresh Token Rotation)
        if (tokenJti == null || !tokenJti.equals(user.getRefreshTokenJti())) {
            // Phát hiện replay attack: refresh token cũ đã bị sử dụng lại!
            // Thu hồi toàn bộ phiên để bảo vệ người dùng
            user.setRefreshTokenJti(null);
            user.setTokenVersion(user.getTokenVersion() + 1);
            userRepository.save(user);
            log.error("CẢNH BÁO BẢO MẬT: Phát hiện Refresh Token Replay Attack đối với user '{}'. Đã thu hồi toàn bộ token!", username);
            throw new BadRequestException("Refresh token không hợp lệ hoặc đã được sử dụng. Vui lòng đăng nhập lại.");
        }

        // 4. Xoay vòng token (Rotation): cấp JTI mới và cập nhật DB
        String newJti = UUID.randomUUID().toString();
        user.setRefreshTokenJti(newJti);
        User updatedUser = userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(updatedUser);
        String newAccessToken = tokenProvider.generateAccessToken(userDetails);
        String newRefreshToken = tokenProvider.generateRefreshToken(userDetails, newJti);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .userId(updatedUser.getId())
                .username(updatedUser.getUsername())
                .email(updatedUser.getEmail())
                .fullName(updatedUser.getFullName())
                .role(updatedUser.getRole().name())
                .build();
    }

    @Transactional
    public void logout(CustomUserDetails currentUser) {
        if (currentUser == null || currentUser.getUser() == null) {
            return;
        }
        User user = currentUser.getUser();
        // Tăng token version để lập tức vô hiệu hóa mọi access token cũ
        user.setTokenVersion(user.getTokenVersion() + 1);
        // Hủy refresh token hiện tại
        user.setRefreshTokenJti(null);
        userRepository.save(user);
        log.info("Người dùng '{}' đã đăng xuất thành công. Đã vô hiệu hóa toàn bộ token.", user.getUsername());
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUserProfile(CustomUserDetails currentUser) {
        if (currentUser == null || currentUser.getUser() == null) {
            throw new BadRequestException("Bạn cần đăng nhập để thực hiện thao tác này.");
        }
        User user = currentUser.getUser();
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
