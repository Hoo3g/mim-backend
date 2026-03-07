package com.hus.mim_backend.infrastructure.adapter.web.profile;

import com.hus.mim_backend.application.profile.dto.ProfileDashboardResponse;
import com.hus.mim_backend.application.profile.dto.ProfileMeResponse;
import com.hus.mim_backend.application.profile.dto.UpdateCompanyProfileRequest;
import com.hus.mim_backend.application.profile.dto.UpdateLecturerProfileRequest;
import com.hus.mim_backend.application.profile.dto.UpdateStudentProfileRequest;
import com.hus.mim_backend.application.profile.usecase.ProfilePortalUseCase;
import com.hus.mim_backend.domain.shared.DomainException;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiEndpoints.PROFILE)
public class ProfileController {
    private final ProfilePortalUseCase profilePortalUseCase;

    public ProfileController(ProfilePortalUseCase profilePortalUseCase) {
        this.profilePortalUseCase = profilePortalUseCase;
    }

    @GetMapping(ApiEndpoints.PROFILE_ME)
    public ResponseEntity<ApiResponse<ProfileMeResponse>> getMyProfile(Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        ProfileMeResponse data = profilePortalUseCase.getMyProfile(email);
        return ResponseEntity.ok(ApiResponse.success(data, "Get profile successfully"));
    }

    @GetMapping(ApiEndpoints.PROFILE_ME_DASHBOARD)
    public ResponseEntity<ApiResponse<ProfileDashboardResponse>> getMyDashboard(Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        ProfileDashboardResponse data = profilePortalUseCase.getMyDashboard(email);
        return ResponseEntity.ok(ApiResponse.success(data, "Get profile dashboard successfully"));
    }

    @PutMapping(ApiEndpoints.PROFILE_ME_STUDENT)
    public ResponseEntity<ApiResponse<ProfileMeResponse>> updateStudentProfile(
            @RequestBody UpdateStudentProfileRequest request,
            Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        ProfileMeResponse data = profilePortalUseCase.updateStudentProfile(email, request);
        return ResponseEntity.ok(ApiResponse.success(data, "Update student profile successfully"));
    }

    @PutMapping(ApiEndpoints.PROFILE_ME_COMPANY)
    public ResponseEntity<ApiResponse<ProfileMeResponse>> updateCompanyProfile(
            @RequestBody UpdateCompanyProfileRequest request,
            Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        ProfileMeResponse data = profilePortalUseCase.updateCompanyProfile(email, request);
        return ResponseEntity.ok(ApiResponse.success(data, "Update company profile successfully"));
    }

    @PutMapping(ApiEndpoints.PROFILE_ME_LECTURER)
    public ResponseEntity<ApiResponse<ProfileMeResponse>> updateLecturerProfile(
            @RequestBody UpdateLecturerProfileRequest request,
            Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        ProfileMeResponse data = profilePortalUseCase.updateLecturerProfile(email, request);
        return ResponseEntity.ok(ApiResponse.success(data, "Update lecturer profile successfully"));
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new DomainException("Authentication required");
        }
        String email = String.valueOf(authentication.getPrincipal());
        if (!StringUtils.hasText(email)) {
            throw new DomainException("Authentication required");
        }
        return email;
    }
}
