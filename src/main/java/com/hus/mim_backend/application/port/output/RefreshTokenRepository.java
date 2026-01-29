package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.domain.auth.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for RefreshToken persistence operations
 */
public interface RefreshTokenRepository {
    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUserId(UUID userId);

    RefreshToken save(RefreshToken refreshToken);

    void revokeByUserId(UUID userId);

    void deleteExpiredTokens();
}
