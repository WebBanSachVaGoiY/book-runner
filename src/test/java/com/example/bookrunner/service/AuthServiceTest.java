package com.example.bookrunner.service;

import com.example.bookrunner.dto.auth.LoginRequest;
import com.example.bookrunner.dto.auth.RefreshTokenRequest;
import com.example.bookrunner.enums.Role;
import com.example.bookrunner.exception.BadRequestException;
import com.example.bookrunner.model.User;
import com.example.bookrunner.repository.CartRepository;
import com.example.bookrunner.repository.UserRepository;
import com.example.bookrunner.security.CustomUserDetails;
import com.example.bookrunner.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .role(Role.ROLE_CUSTOMER)
                .enabled(true)
                .tokenVersion(1L)
                .refreshTokenJti("current-valid-jti")
                .build();
        userDetails = new CustomUserDetails(testUser);
    }

    @Test
    @DisplayName("Login không gây User Enumeration khi sai thông tin đăng nhập")
    void testLogin_NoUserEnumeration() {
        LoginRequest request = new LoginRequest("NotExistingUser", "wrongPass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(userRepository, never()).findByUsername(any());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Phát hiện Token Replay Attack khi JTI refresh token không khớp với DB")
    void testRefreshToken_ReplayAttackDetection() {
        RefreshTokenRequest request = new RefreshTokenRequest("stolen_or_old_token");

        when(tokenProvider.validateRefreshToken("stolen_or_old_token")).thenReturn(true);
        when(tokenProvider.getUsernameFromToken("stolen_or_old_token")).thenReturn("john_doe");
        when(tokenProvider.getJtiFromToken("stolen_or_old_token")).thenReturn("old-or-replayed-jti");
        when(tokenProvider.getTokenVersionFromToken("stolen_or_old_token")).thenReturn(1L);
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));

        // DB đang lưu "current-valid-jti", nhưng token gửi lên mang "old-or-replayed-jti"
        assertThrows(BadRequestException.class, () -> authService.refreshToken(request));

        // Xác nhận đã thu hồi toàn bộ token của người dùng (tăng version, xóa JTI)
        assertEquals(2L, testUser.getTokenVersion());
        assertNull(testUser.getRefreshTokenJti());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Logout tăng tokenVersion và xóa refreshTokenJti")
    void testLogout_RevokesTokens() {
        authService.logout(userDetails);

        assertEquals(2L, testUser.getTokenVersion());
        assertNull(testUser.getRefreshTokenJti());
        verify(userRepository).save(testUser);
    }
}
