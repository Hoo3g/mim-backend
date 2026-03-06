package com.hus.mim_backend.application.auth.usecase;

import com.hus.mim_backend.application.auth.dto.AuthResponse;
import com.hus.mim_backend.application.auth.dto.GoogleLoginRequest;

/**
 * Input port for Google login use case
 */
public interface GoogleLoginUseCase {
    AuthResponse loginWithGoogle(GoogleLoginRequest request);
}
