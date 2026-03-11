package com.hus.mim_backend.application.profile.service;

import com.hus.mim_backend.application.port.output.CompanyRepository;
import com.hus.mim_backend.application.profile.dto.CompanyProfileResponse;
import com.hus.mim_backend.application.profile.dto.UpdateCompanyProfileRequest;
import com.hus.mim_backend.application.profile.usecase.ManageCompanyProfileUseCase;
import com.hus.mim_backend.domain.profile.model.Company;
import com.hus.mim_backend.domain.shared.DomainException;

import java.time.LocalDateTime;
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
        if (userId == null) {
            throw new DomainException("userId is required");
        }
        Company company = companyRepository.findById(userId)
                .orElseThrow(() -> new DomainException("Company profile not found"));
        return toResponse(company);
    }

    @Override
    public CompanyProfileResponse updateProfile(UUID userId, UpdateCompanyProfileRequest request) {
        if (userId == null) {
            throw new DomainException("userId is required");
        }
        UpdateCompanyProfileRequest safeRequest = request == null ? new UpdateCompanyProfileRequest() : request;

        Company company = companyRepository.findById(userId)
                .orElseGet(() -> Company.builder().id(userId).build());

        if (safeRequest.getName() != null) {
            company.setName(trimToNull(safeRequest.getName()));
        }
        if (safeRequest.getIndustry() != null) {
            company.setIndustry(trimToNull(safeRequest.getIndustry()));
        }
        if (safeRequest.getWebsite() != null) {
            company.setWebsite(trimToNull(safeRequest.getWebsite()));
        }
        if (safeRequest.getLocation() != null) {
            company.setLocation(trimToNull(safeRequest.getLocation()));
        }
        if (safeRequest.getDescription() != null) {
            company.setDescription(trimToNull(safeRequest.getDescription()));
        }
        company.setUpdatedAt(LocalDateTime.now());

        Company saved = companyRepository.save(company);
        return toResponse(saved);
    }

    private CompanyProfileResponse toResponse(Company company) {
        CompanyProfileResponse response = new CompanyProfileResponse();
        response.setId(company.getId());
        response.setName(company.getName());
        response.setIndustry(company.getIndustry());
        response.setWebsite(company.getWebsite());
        response.setLocation(company.getLocation());
        response.setDescription(company.getDescription());
        return response;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
