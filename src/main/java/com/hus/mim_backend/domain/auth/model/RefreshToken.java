package com.hus.mim_backend.domain.auth.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RefreshToken entity for JWT refresh token management
 */
@Getter
@Setter
public class RefreshToken {
    private UUID id;
    private UUID userId;
    private String token;
    private LocalDateTime expiryDate;
    private boolean revoked;
    private LocalDateTime createdAt;

    public RefreshToken() {
    }

    public static RefreshToken issue(UUID userId, String token, LocalDateTime expiryDate) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUserId(userId);
        refreshToken.setToken(token);
        refreshToken.setExpiryDate(expiryDate);
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(LocalDateTime.now());
        return refreshToken;
    }

    public boolean isExpired() {
        return expiryDate != null && LocalDateTime.now().isAfter(expiryDate);
    }

    public boolean isActive() {
        return !revoked && !isExpired();
    }

    public void revoke() {
        this.revoked = true;
    }
}
