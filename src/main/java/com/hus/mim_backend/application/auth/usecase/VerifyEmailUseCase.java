package com.hus.mim_backend.application.auth.usecase;

import com.hus.mim_backend.application.auth.dto.UserResponse;

public interface VerifyEmailUseCase {
    UserResponse verifyEmail(String token);
}
