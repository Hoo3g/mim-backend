package com.hus.mim_backend.application.profile.service;

import com.hus.mim_backend.application.port.output.ProfilePortalRepository;
import com.hus.mim_backend.application.port.output.SpecializationRepository;
import com.hus.mim_backend.application.profile.dto.ProfileDashboardResponse;
import com.hus.mim_backend.application.profile.dto.ProfileMeResponse;
import com.hus.mim_backend.application.profile.dto.UpdateCompanyProfileRequest;
import com.hus.mim_backend.application.profile.dto.UpdateLecturerProfileRequest;
import com.hus.mim_backend.application.profile.dto.UpdateStudentProfileRequest;
import com.hus.mim_backend.application.profile.usecase.ProfilePortalUseCase;
import com.hus.mim_backend.domain.shared.DomainException;
import org.springframework.util.StringUtils;

import java.util.UUID;

public class ProfilePortalService implements ProfilePortalUseCase {
    private static final String ROLE_STUDENT = "STUDENT";
    private static final String ROLE_COMPANY = "COMPANY";
    private static final String ROLE_LECTURER = "LECTURER";
    private static final String ROLE_ADMIN = "ADMIN";

    private final ProfilePortalRepository repository;
    private final SpecializationRepository specializationRepository;

    public ProfilePortalService(ProfilePortalRepository repository,
            SpecializationRepository specializationRepository) {
        this.repository = repository;
        this.specializationRepository = specializationRepository;
    }

    @Override
    public ProfileMeResponse getMyProfile(String email) {
        ProfileMeResponse profile = resolveProfile(email);
        profile.setRole(normalizeRole(profile.getRole()));
        return profile;
    }

    @Override
    public ProfileDashboardResponse getMyDashboard(String email) {
        ProfileMeResponse profile = resolveProfile(email);
        UUID userId = profile.getUserId();
        String role = normalizeRole(profile.getRole());

        ProfileDashboardResponse response = new ProfileDashboardResponse();
        response.setRole(role);

        if (ROLE_STUDENT.equals(role)) {
            response.setStudent(repository.getStudentDashboard(userId));
        } else if (ROLE_COMPANY.equals(role)) {
            response.setCompany(repository.getCompanyDashboard(userId));
        } else if (ROLE_LECTURER.equals(role)) {
            response.setLecturer(repository.getLecturerDashboard(userId));
        }

        return response;
    }

    @Override
    public ProfileMeResponse updateStudentProfile(String email, UpdateStudentProfileRequest request) {
        UUID userId = resolveUserId(email);
        ensureRole(userId, ROLE_STUDENT);
        UpdateStudentProfileRequest sanitized = request == null ? new UpdateStudentProfileRequest() : request;
        validateStudentMajor(sanitized);
        repository.upsertStudentProfile(userId, sanitized);
        return getMyProfile(email);
    }

    @Override
    public ProfileMeResponse updateCompanyProfile(String email, UpdateCompanyProfileRequest request) {
        UUID userId = resolveUserId(email);
        ensureRole(userId, ROLE_COMPANY);
        repository.upsertCompanyProfile(userId, request == null ? new UpdateCompanyProfileRequest() : request);
        return getMyProfile(email);
    }

    @Override
    public ProfileMeResponse updateLecturerProfile(String email, UpdateLecturerProfileRequest request) {
        UUID userId = resolveUserId(email);
        ensureRole(userId, ROLE_LECTURER);
        repository.upsertLecturerProfile(userId, request == null ? new UpdateLecturerProfileRequest() : request);
        return getMyProfile(email);
    }

    @Override
    public ProfileMeResponse updateStudentDefaultCv(String email, String cvUrl) {
        UUID userId = resolveUserId(email);
        ensureRole(userId, ROLE_STUDENT);
        if (!StringUtils.hasText(cvUrl)) {
            throw new DomainException("CV URL is required");
        }
        repository.updateStudentCv(userId, cvUrl.trim());
        return getMyProfile(email);
    }

    @Override
    public ProfileMeResponse updateUserAvatar(String email, String avatarUrl) {
        UUID userId = resolveUserId(email);
        if (!StringUtils.hasText(avatarUrl)) {
            throw new DomainException("Avatar URL is required");
        }
        repository.updateUserAvatar(userId, avatarUrl.trim());
        return getMyProfile(email);
    }

    private ProfileMeResponse resolveProfile(String email) {
        if (!StringUtils.hasText(email)) {
            throw new DomainException("Authentication required");
        }

        return repository.findProfileByEmail(email.trim())
                .orElseThrow(() -> new DomainException("Profile not found"));
    }

    private UUID resolveUserId(String email) {
        if (!StringUtils.hasText(email)) {
            throw new DomainException("Authentication required");
        }
        return repository.findUserIdByEmail(email.trim())
                .orElseThrow(() -> new DomainException("Authenticated user is not found"));
    }

    private void ensureRole(UUID userId, String expectedRole) {
        String role = repository.findPrimaryRole(userId).orElse(ROLE_STUDENT);
        if (!expectedRole.equalsIgnoreCase(role)) {
            throw new DomainException("Current account does not match target profile type");
        }
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return ROLE_STUDENT;
        }
        String normalized = role.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        if (ROLE_COMPANY.equals(normalized)
                || ROLE_LECTURER.equals(normalized)
                || ROLE_STUDENT.equals(normalized)
                || ROLE_ADMIN.equals(normalized)) {
            return normalized;
        }
        return ROLE_STUDENT;
    }

    private void validateStudentMajor(UpdateStudentProfileRequest request) {
        if (request == null || !StringUtils.hasText(request.getMajor())) {
            return;
        }

        String major = request.getMajor().trim();
        boolean valid = specializationRepository.findActiveSpecializationName(major).isPresent();
        if (!valid) {
            throw new DomainException("Major is invalid or inactive. Please choose from admin-managed specializations.");
        }
    }
}
