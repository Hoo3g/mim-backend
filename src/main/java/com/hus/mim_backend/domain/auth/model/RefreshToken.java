package com.hus.mim_backend.domain.auth.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RefreshToken entity for JWT refresh token management
 */
public class RefreshToken {
    private UUID id;
    private UUID userId;
    private String token;
    private LocalDateTime expiryDate;
    private boolean revoked;
    private LocalDateTime createdAt;

    // TODO: Implement getters/setters
    // TODO: Implement builder pattern
    // TODO: Implement isExpired() domain logic
    // TODO: Implement revoke() domain logic
}
