package com.hus.mim_backend.application.auth.usecase;

import com.hus.mim_backend.application.auth.dto.RegisterRequest;
import com.hus.mim_backend.application.auth.dto.UserResponse;

import java.util.UUID;

public interface AdminProvisionUserUseCase {
    UserResponse createUserByAdmin(RegisterRequest request);

    UserResponse lockUserByAdmin(String actorEmail, UUID userId);

    UserResponse unlockUserByAdmin(String actorEmail, UUID userId);

    void deleteUserByAdmin(String actorEmail, UUID userId);
}
