package com.hus.mim_backend.infrastructure.adapter.persistence.auth;

import com.hus.mim_backend.application.port.output.EmailVerificationTokenRepository;
import com.hus.mim_backend.domain.auth.model.EmailVerificationToken;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class EmailVerificationTokenJpaAdapter implements EmailVerificationTokenRepository {
    private final EmailVerificationTokenJpaRepository repository;

    public EmailVerificationTokenJpaAdapter(EmailVerificationTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<EmailVerificationToken> findByToken(String token) {
        return repository.findByToken(token).map(EmailVerificationTokenEntity::toDomain);
    }

    @Override
    public EmailVerificationToken save(EmailVerificationToken token) {
        return repository.save(EmailVerificationTokenEntity.fromDomain(token)).toDomain();
    }

    @Override
    public void deleteByUserId(UUID userId) {
        repository.deleteByUserId(userId);
    }

    @Override
    public void deleteByToken(String token) {
        repository.deleteByToken(token);
    }

    @Override
    public void deleteExpiredTokens() {
        repository.deleteByExpiryDateBefore(LocalDateTime.now());
    }
}
