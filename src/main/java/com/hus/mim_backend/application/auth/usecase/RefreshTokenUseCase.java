package com.hus.mim_backend.application.auth.usecase;

import com.hus.mim_backend.application.auth.dto.AuthResponse;

/**
 * Input port for refresh token use case
 */
public interface RefreshTokenUseCase {
    AuthResponse refreshToken(String refreshToken);
}
