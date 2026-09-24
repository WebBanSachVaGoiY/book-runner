package com.example.bookrunner.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
public class JwtTokenProvider {

    public static final String CLAIM_TOKEN_TYPE = "type";
    public static final String TYPE_ACCESS = "ACCESS";
    public static final String TYPE_REFRESH = "REFRESH";
    public static final String ISSUER = "book-runner";

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationInMs;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationInMs;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException("JWT Secret không được để trống. Vui lòng thiết lập biến môi trường JWT_SECRET (tối thiểu 32 ký tự / 256 bits).");
        }
        byte[] keyBytes;
        // Quy ước tiền tố base64: để loại bỏ hoàn toàn tính mơ hồ (ambiguity) khi decode key
        if (jwtSecret.startsWith("base64:")) {
            String base64Key = jwtSecret.substring(7);
            try {
                keyBytes = Decoders.BASE64.decode(base64Key);
            } catch (Exception e) {
                throw new IllegalStateException("JWT Secret có tiền tố 'base64:' nhưng định dạng không phải Base64 hợp lệ.", e);
            }
        } else {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT Secret quá ngắn (" + keyBytes.length + " bytes). Yêu cầu tối thiểu 32 bytes (256 bits) cho thuật toán HMAC-SHA256.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(CustomUserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .issuer(ISSUER)
                .subject(userDetails.getUsername())
                .claim("userId", userDetails.getId())
                .claim("email", userDetails.getEmail())
                // UI hint only - server-side authorization must always check database authorities
                .claim("role", userDetails.getUser().getRole().name())
                .claim(CLAIM_TOKEN_TYPE, TYPE_ACCESS)
                .claim("v", userDetails.getTokenVersion())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(CustomUserDetails userDetails, String jti) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationInMs);

        return Jwts.builder()
                .id(jti)
                .issuer(ISSUER)
                .subject(userDetails.getUsername())
                .claim("userId", userDetails.getId())
                .claim(CLAIM_TOKEN_TYPE, TYPE_REFRESH)
                .claim("v", userDetails.getTokenVersion())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        return validateTokenType(token, TYPE_ACCESS);
    }

    public boolean validateRefreshToken(String token) {
        return validateTokenType(token, TYPE_REFRESH);
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token đã hết hạn: {}", ex.getMessage());
        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException ex) {
            log.error("JWT token không hợp lệ: {}", ex.getMessage());
        }
        return false;
    }

    private boolean validateTokenType(String token, String expectedType) {
        try {
            Claims claims = getClaims(token);
            String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (!expectedType.equals(tokenType)) {
                log.warn("JWT token không đúng loại. Yêu cầu: {}, Nhận được: {}", expectedType, tokenType);
                return false;
            }
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token đã hết hạn: {}", ex.getMessage());
        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException ex) {
            log.error("JWT token không hợp lệ: {}", ex.getMessage());
        }
        return false;
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getJtiFromToken(String token) {
        return getClaims(token).getId();
    }

    public long getTokenVersionFromToken(String token) {
        try {
            Claims claims = getClaims(token);
            Object versionObj = claims.get("v");
            if (versionObj instanceof Number) {
                return ((Number) versionObj).longValue();
            }
        } catch (Exception ignored) {
        }
        return 0L;
    }

    public Optional<Long> getUserIdFromToken(String token) {
        try {
            Claims claims = getClaims(token);
            Object userIdObj = claims.get("userId");
            if (userIdObj instanceof Number) {
                return Optional.of(((Number) userIdObj).longValue());
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    public Long getRequiredUserIdFromToken(String token) {
        return getUserIdFromToken(token)
                .orElseThrow(() -> new JwtException("Không tìm thấy userId hợp lệ trong JWT token"));
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER) // Bắt buộc kiểm tra issuer, chống token cross-environment
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
