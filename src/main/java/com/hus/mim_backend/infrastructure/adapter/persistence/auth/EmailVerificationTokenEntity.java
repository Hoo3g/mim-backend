package com.hus.mim_backend.infrastructure.adapter.persistence.auth;

import com.hus.mim_backend.domain.auth.model.EmailVerificationToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationTokenEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public EmailVerificationToken toDomain() {
        EmailVerificationToken domain = new EmailVerificationToken();
        domain.setId(id);
        domain.setUserId(userId);
        domain.setToken(token);
        domain.setExpiryDate(expiryDate);
        domain.setCreatedAt(createdAt);
        return domain;
    }

    public static EmailVerificationTokenEntity fromDomain(EmailVerificationToken token) {
        EmailVerificationTokenEntity entity = new EmailVerificationTokenEntity();
        entity.setId(token.getId());
        entity.setUserId(token.getUserId());
        entity.setToken(token.getToken());
        entity.setExpiryDate(token.getExpiryDate());
        entity.setCreatedAt(token.getCreatedAt());
        return entity;
    }
}
