package com.hus.mim_backend.application.profile.usecase;

import com.hus.mim_backend.application.profile.dto.LecturerProfileResponse;
import com.hus.mim_backend.application.profile.dto.UpdateLecturerProfileRequest;
import java.util.UUID;

/**
 * Input port for managing lecturer profiles
 */
public interface ManageLecturerProfileUseCase {
    LecturerProfileResponse getProfile(UUID userId);

    LecturerProfileResponse updateProfile(UUID userId, UpdateLecturerProfileRequest request);
}
