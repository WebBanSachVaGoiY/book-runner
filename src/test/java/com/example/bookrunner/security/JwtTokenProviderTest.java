package com.example.bookrunner.security;

import com.example.bookrunner.enums.Role;
import com.example.bookrunner.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private CustomUserDetails userDetails;
    private static final String TEST_SECRET = "1234567890123456789012345678901234567890123456789012345678901234";

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(tokenProvider, "jwtExpirationInMs", 3600000L);
        ReflectionTestUtils.setField(tokenProvider, "refreshExpirationInMs", 604800000L);
        tokenProvider.init();

        User user = User.builder()
                .id(100L)
                .username("testuser")
                .email("test@example.com")
                .role(Role.ROLE_CUSTOMER)
                .enabled(true)
                .tokenVersion(1L)
                .build();
        userDetails = new CustomUserDetails(user);
    }

    @Test
    @DisplayName("Fail-fast khi secret rỗng hoặc quá ngắn")
    void testFailFastOnInvalidSecret() {
        JwtTokenProvider badProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(badProvider, "jwtSecret", "");
        assertThrows(IllegalStateException.class, badProvider::init);

        ReflectionTestUtils.setField(badProvider, "jwtSecret", "too-short-secret");
        assertThrows(IllegalStateException.class, badProvider::init);
    }

    @Test
    @DisplayName("Quy ước tiền tố base64: giải mã đúng secret Base64")
    void testBase64PrefixConvention() {
        byte[] rawKey = "12345678901234567890123456789012".getBytes(StandardCharsets.UTF_8); // 32 bytes
        String base64Encoded = Base64.getEncoder().encodeToString(rawKey);

        JwtTokenProvider b64Provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(b64Provider, "jwtSecret", "base64:" + base64Encoded);
        ReflectionTestUtils.setField(b64Provider, "jwtExpirationInMs", 3600000L);
        ReflectionTestUtils.setField(b64Provider, "refreshExpirationInMs", 604800000L);
        assertDoesNotThrow(b64Provider::init);

        String token = b64Provider.generateAccessToken(userDetails);
        assertTrue(b64Provider.validateAccessToken(token));
    }

    @Test
    @DisplayName("Access token có type=ACCESS và không dùng được như refresh token")
    void testAccessTokenValidation() {
        String accessToken = tokenProvider.generateAccessToken(userDetails);

        assertNotNull(accessToken);
        assertTrue(tokenProvider.validateAccessToken(accessToken));
        assertFalse(tokenProvider.validateRefreshToken(accessToken), "Access token không được coi là refresh token");

        assertEquals("testuser", tokenProvider.getUsernameFromToken(accessToken));
        assertEquals(Optional.of(100L), tokenProvider.getUserIdFromToken(accessToken));
        assertEquals(1L, tokenProvider.getTokenVersionFromToken(accessToken));
    }

    @Test
    @DisplayName("Refresh token có type=REFRESH và không dùng được như access token")
    void testRefreshTokenValidation() {
        String jti = UUID.randomUUID().toString();
        String refreshToken = tokenProvider.generateRefreshToken(userDetails, jti);

        assertNotNull(refreshToken);
        assertTrue(tokenProvider.validateRefreshToken(refreshToken));
        assertFalse(tokenProvider.validateAccessToken(refreshToken), "Refresh token không được dùng làm access token để gọi API");

        assertEquals("testuser", tokenProvider.getUsernameFromToken(refreshToken));
        assertEquals(jti, tokenProvider.getJtiFromToken(refreshToken));
        assertEquals(1L, tokenProvider.getTokenVersionFromToken(refreshToken));
    }
}
