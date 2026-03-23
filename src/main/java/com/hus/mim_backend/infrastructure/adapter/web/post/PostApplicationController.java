package com.hus.mim_backend.infrastructure.adapter.web.post;

import com.hus.mim_backend.application.post.dto.ApplicationRequest;
import com.hus.mim_backend.application.post.dto.ApplicationResponse;
import com.hus.mim_backend.application.post.dto.PendingApplicantResponse;
import com.hus.mim_backend.application.post.dto.PendingApplicationResponse;
import com.hus.mim_backend.application.post.usecase.ApplicationPortalUseCase;
import com.hus.mim_backend.domain.shared.DomainException;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiEndpoints.POSTS)
public class PostApplicationController {
    private final ApplicationPortalUseCase applicationPortalUseCase;

    public PostApplicationController(ApplicationPortalUseCase applicationPortalUseCase) {
        this.applicationPortalUseCase = applicationPortalUseCase;
    }

    @PostMapping(ApiEndpoints.POST_APPLY)
    public ResponseEntity<ApiResponse<ApplicationResponse>> applyToPost(
            @PathVariable UUID postId,
            @RequestBody(required = false) ApplicationRequest request,
            Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        ApplicationResponse response = applicationPortalUseCase.applyToPost(email, postId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Apply successfully"));
    }

    @DeleteMapping(ApiEndpoints.POST_APPLY)
    public ResponseEntity<ApiResponse<Boolean>> cancelApplication(
            @PathVariable UUID postId,
            Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        boolean cancelled = applicationPortalUseCase.cancelApplication(email, postId);
        return ResponseEntity.ok(ApiResponse.success(cancelled, "Cancel application successfully"));
    }

    @GetMapping(ApiEndpoints.POST_APPLICATIONS_MY)
    public ResponseEntity<ApiResponse<List<PendingApplicationResponse>>> getMyPendingApplications(
            @RequestParam(required = false, defaultValue = "PENDING") String status,
            Authentication authentication) {
        if (!"PENDING".equalsIgnoreCase(status)) {
            throw new DomainException("Only PENDING status is supported in this endpoint");
        }
        String email = resolveAuthenticatedEmail(authentication);
        List<PendingApplicationResponse> data = applicationPortalUseCase.getMyPendingApplications(email);
        return ResponseEntity.ok(ApiResponse.success(data, "Get pending applications successfully"));
    }

    @GetMapping(ApiEndpoints.POST_APPLICATIONS_RECEIVED)
    public ResponseEntity<ApiResponse<List<PendingApplicantResponse>>> getPendingApplicants(
            @RequestParam(required = false, defaultValue = "PENDING") String status,
            Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        List<PendingApplicantResponse> data = applicationPortalUseCase.getApplicantsForMyCompanyPosts(email, status);
        return ResponseEntity.ok(ApiResponse.success(data, "Get pending applicants successfully"));
    }

    @DeleteMapping(ApiEndpoints.POST_APPLICATION_BY_ID)
    public ResponseEntity<ApiResponse<Boolean>> deleteApplication(
            @PathVariable UUID applicationId,
            Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        boolean deleted = applicationPortalUseCase.deleteApplicationForMyCompanyPost(email, applicationId);
        return ResponseEntity.ok(ApiResponse.success(deleted, "Delete applicant successfully"));
    }

    @PatchMapping(ApiEndpoints.POST_APPLICATION_STATUS)
    public ResponseEntity<ApiResponse<Boolean>> updateApplicationStatus(
            @PathVariable UUID applicationId,
            @RequestParam String status,
            Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        boolean updated = applicationPortalUseCase.updateApplicationStatusForMyCompanyPost(email, applicationId, status);
        return ResponseEntity.ok(ApiResponse.success(updated, "Update applicant status successfully"));
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
