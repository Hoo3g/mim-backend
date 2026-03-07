package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.application.profile.dto.ProfileDashboardResponse;
import com.hus.mim_backend.application.profile.dto.ProfileMeResponse;
import com.hus.mim_backend.application.profile.dto.UpdateCompanyProfileRequest;
import com.hus.mim_backend.application.profile.dto.UpdateLecturerProfileRequest;
import com.hus.mim_backend.application.profile.dto.UpdateStudentProfileRequest;

import java.util.Optional;
import java.util.UUID;

public interface ProfilePortalRepository {
    Optional<ProfileMeResponse> findProfileByEmail(String email);

    Optional<UUID> findUserIdByEmail(String email);

    Optional<String> findPrimaryRole(UUID userId);

    ProfileDashboardResponse.StudentDashboard getStudentDashboard(UUID userId);

    ProfileDashboardResponse.CompanyDashboard getCompanyDashboard(UUID userId);

    ProfileDashboardResponse.LecturerDashboard getLecturerDashboard(UUID userId);

    void upsertStudentProfile(UUID userId, UpdateStudentProfileRequest request);

    void upsertCompanyProfile(UUID userId, UpdateCompanyProfileRequest request);

    void upsertLecturerProfile(UUID userId, UpdateLecturerProfileRequest request);

    void updateStudentCv(UUID userId, String cvUrl);

    void updateUserAvatar(UUID userId, String avatarUrl);
}
