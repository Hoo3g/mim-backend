package com.hus.mim_backend.application.auth.usecase;

import com.hus.mim_backend.application.auth.dto.RegisterRequest;
import com.hus.mim_backend.application.auth.dto.UserResponse;

public interface AdminProvisionUserUseCase {
    UserResponse createUserByAdmin(RegisterRequest request);
}
