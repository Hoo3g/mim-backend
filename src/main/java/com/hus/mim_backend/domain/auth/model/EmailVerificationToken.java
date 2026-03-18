package com.hus.mim_backend.domain.auth.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Token used to confirm ownership of an email address.
 */
@Setter
@Getter
public class EmailVerificationToken {
    private UUID id;
    private UUID userId;
    private String token;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;

    public static EmailVerificationToken issue(UUID userId, String token, LocalDateTime expiryDate) {
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setId(UUID.randomUUID());
        verificationToken.setUserId(userId);
        verificationToken.setToken(token);
        verificationToken.setExpiryDate(expiryDate);
        verificationToken.setCreatedAt(LocalDateTime.now());
        return verificationToken;
    }

    public boolean isExpired() {
        return expiryDate == null || expiryDate.isBefore(LocalDateTime.now());
    }
}
