package com.hus.mim_backend.application.profile.usecase;

import com.hus.mim_backend.application.profile.dto.ProfileDashboardResponse;
import com.hus.mim_backend.application.profile.dto.ProfileMeResponse;
import com.hus.mim_backend.application.profile.dto.UpdateCompanyProfileRequest;
import com.hus.mim_backend.application.profile.dto.UpdateLecturerProfileRequest;
import com.hus.mim_backend.application.profile.dto.UpdateStudentProfileRequest;

public interface ProfilePortalUseCase {
    ProfileMeResponse getMyProfile(String email);

    ProfileDashboardResponse getMyDashboard(String email);

    ProfileMeResponse updateStudentProfile(String email, UpdateStudentProfileRequest request);

    ProfileMeResponse updateCompanyProfile(String email, UpdateCompanyProfileRequest request);

    ProfileMeResponse updateLecturerProfile(String email, UpdateLecturerProfileRequest request);

    ProfileMeResponse updateStudentDefaultCv(String email, String cvUrl);

    ProfileMeResponse updateUserAvatar(String email, String avatarUrl);
}
