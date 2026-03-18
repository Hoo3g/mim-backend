package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.domain.auth.model.EmailVerificationToken;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for email verification tokens.
 */
public interface EmailVerificationTokenRepository {
    Optional<EmailVerificationToken> findByToken(String token);

    EmailVerificationToken save(EmailVerificationToken token);

    void deleteByUserId(UUID userId);

    void deleteByToken(String token);

    void deleteExpiredTokens();
}
