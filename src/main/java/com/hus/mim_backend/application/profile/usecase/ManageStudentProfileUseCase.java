package com.hus.mim_backend.application.profile.usecase;

import com.hus.mim_backend.application.profile.dto.StudentProfileResponse;
import com.hus.mim_backend.application.profile.dto.UpdateStudentProfileRequest;
import java.util.UUID;

/**
 * Input port for managing student profiles
 */
public interface ManageStudentProfileUseCase {
    StudentProfileResponse getProfile(UUID userId);

    StudentProfileResponse updateProfile(UUID userId, UpdateStudentProfileRequest request);
}
