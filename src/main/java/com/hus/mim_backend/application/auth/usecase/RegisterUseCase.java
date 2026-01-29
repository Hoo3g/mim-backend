package com.hus.mim_backend.application.auth.usecase;

import com.hus.mim_backend.application.auth.dto.RegisterRequest;
import com.hus.mim_backend.application.auth.dto.UserResponse;

/**
 * Input port for registration use case
 */
public interface RegisterUseCase {
    UserResponse register(RegisterRequest request);
}
