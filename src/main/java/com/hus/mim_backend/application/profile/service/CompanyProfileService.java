package com.hus.mim_backend.application.profile.service;

import com.hus.mim_backend.application.port.output.CompanyRepository;
import com.hus.mim_backend.application.profile.dto.CompanyProfileResponse;
import com.hus.mim_backend.application.profile.dto.UpdateCompanyProfileRequest;
import com.hus.mim_backend.application.profile.usecase.ManageCompanyProfileUseCase;

import java.util.UUID;

/**
 * Service for company profile management.
 */
public class CompanyProfileService implements ManageCompanyProfileUseCase {

    private final CompanyRepository companyRepository;

    public CompanyProfileService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Override
    public CompanyProfileResponse getProfile(UUID userId) {
        // TODO: fetch company and map to response
        return null;
    }

    @Override
    public CompanyProfileResponse updateProfile(UUID userId, UpdateCompanyProfileRequest request) {
        // TODO: update company fields and persist
        return null;
    }
}
