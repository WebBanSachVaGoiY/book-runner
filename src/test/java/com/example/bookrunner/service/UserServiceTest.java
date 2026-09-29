package com.example.bookrunner.service;

import com.example.bookrunner.dto.auth.UserSummaryResponse;
import com.example.bookrunner.dto.request.ChangePasswordRequest;
import com.example.bookrunner.dto.request.UpdateProfileRequest;
import com.example.bookrunner.enums.Role;
import com.example.bookrunner.exception.BadRequestException;
import com.example.bookrunner.exception.ItemNotFoundException;
import com.example.bookrunner.model.User;
import com.example.bookrunner.repository.UserRepository;
import com.example.bookrunner.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("nguyenvana")
                .email("vana@example.com")
                .password("$2a$10$encodedOldPassword")
                .fullName("Nguyễn Văn A")
                .phone("0912345678")
                .address("123 Hà Nội")
                .role(Role.ROLE_CUSTOMER)
                .enabled(true)
                .tokenVersion(1L)
                .refreshTokenJti("jti-123")
                .build();
    }

    @Test
    @DisplayName("getProfile - Trả về thông tin hồ sơ người dùng thành công")
    void getProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserSummaryResponse response = userService.getProfile(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("nguyenvana", response.getUsername());
        assertEquals("vana@example.com", response.getEmail());
        assertEquals("Nguyễn Văn A", response.getFullName());
    }

    @Test
    @DisplayName("updateProfile - Cập nhật thông tin cá nhân thành công")
    void updateProfile_Success() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyễn Văn B")
                .phone("0987654321")
                .address("456 TP.HCM")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSummaryResponse response = userService.updateProfile(1L, request);

        assertNotNull(response);
        assertEquals("Nguyễn Văn B", response.getFullName());
        assertEquals("0987654321", response.getPhone());
        assertEquals("456 TP.HCM", response.getAddress());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("changePassword - Đổi mật khẩu thành công và tăng tokenVersion để thu hồi phiên cũ")
    void changePassword_Success() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("CurrentPass123")
                .newPassword("NewPass12345")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("CurrentPass123", "$2a$10$encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("NewPass12345", "$2a$10$encodedOldPassword")).thenReturn(false);
        when(passwordEncoder.encode("NewPass12345")).thenReturn("$2a$10$encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.changePassword(1L, request);

        assertEquals("$2a$10$encodedNewPassword", testUser.getPassword());
        assertEquals(2L, testUser.getTokenVersion(), "tokenVersion phải được tăng lên 2 để vô hiệu hóa token cũ");
        assertNull(testUser.getRefreshTokenJti(), "refreshTokenJti phải được reset về null");
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("changePassword - Ném BadRequestException khi mật khẩu hiện tại không đúng")
    void changePassword_WrongCurrentPassword() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPass123")
                .newPassword("NewPass12345")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPass123", "$2a$10$encodedOldPassword")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> userService.changePassword(1L, request));
        assertEquals("Mật khẩu hiện tại không chính xác", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("toggleUserEnabled - Khóa tài khoản thành công và thu hồi phiên đăng nhập")
    void toggleUserEnabled_DisableUser_RevokesTokens() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSummaryResponse response = userService.toggleUserEnabled(1L);

        assertNotNull(response);
        assertFalse(testUser.getEnabled(), "Tài khoản phải chuyển sang vô hiệu hóa");
        assertEquals(2L, testUser.getTokenVersion(), "tokenVersion phải tăng để vô hiệu hóa token khi tài khoản bị khóa");
        assertNull(testUser.getRefreshTokenJti(), "refresh token phải bị xóa");
        verify(userRepository, times(1)).save(testUser);
    }
}
