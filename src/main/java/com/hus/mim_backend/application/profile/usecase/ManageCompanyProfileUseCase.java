package com.hus.mim_backend.application.profile.usecase;

import com.hus.mim_backend.application.profile.dto.CompanyProfileResponse;
import com.hus.mim_backend.application.profile.dto.UpdateCompanyProfileRequest;
import java.util.UUID;

/**
 * Input port for managing company profiles
 */
public interface ManageCompanyProfileUseCase {
    CompanyProfileResponse getProfile(UUID userId);

    CompanyProfileResponse updateProfile(UUID userId, UpdateCompanyProfileRequest request);
}
