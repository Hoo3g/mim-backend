package com.hus.mim_backend.application.auth.usecase;

import com.hus.mim_backend.application.auth.dto.AuthResponse;
import com.hus.mim_backend.application.auth.dto.LoginRequest;

/**
 * Input port for login use case
 */
public interface LoginUseCase {
    AuthResponse login(LoginRequest request);
}
