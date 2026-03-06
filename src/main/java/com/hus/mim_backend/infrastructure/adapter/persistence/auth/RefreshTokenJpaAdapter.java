package com.hus.mim_backend.infrastructure.adapter.persistence.auth;

import com.hus.mim_backend.application.port.output.RefreshTokenRepository;
import com.hus.mim_backend.domain.auth.model.RefreshToken;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter implementing refresh token persistence port.
 */
@Component
public class RefreshTokenJpaAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    public RefreshTokenJpaAdapter(RefreshTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token).map(RefreshTokenEntity::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByUserId(UUID userId) {
        return jpaRepository.findTopByUserIdOrderByCreatedAtDesc(userId).map(RefreshTokenEntity::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenEntity saved = jpaRepository.save(RefreshTokenEntity.fromDomain(refreshToken));
        return saved.toDomain();
    }

    @Override
    public void revokeByUserId(UUID userId) {
        jpaRepository.revokeByUserId(userId);
    }

    @Override
    public void deleteExpiredTokens() {
        jpaRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }
}
