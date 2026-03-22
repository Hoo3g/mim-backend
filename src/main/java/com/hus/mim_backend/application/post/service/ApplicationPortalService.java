package com.hus.mim_backend.application.post.service;

import com.hus.mim_backend.application.port.output.ApplicationPortalRepository;
import com.hus.mim_backend.application.post.dto.ApplicationRequest;
import com.hus.mim_backend.application.post.dto.ApplicationResponse;
import com.hus.mim_backend.application.post.dto.PendingApplicantResponse;
import com.hus.mim_backend.application.post.dto.PendingApplicationResponse;
import com.hus.mim_backend.application.post.usecase.ApplicationPortalUseCase;
import com.hus.mim_backend.domain.shared.DomainException;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ApplicationPortalService implements ApplicationPortalUseCase {
    private static final String ROLE_STUDENT = "STUDENT";
    private static final String ROLE_COMPANY = "COMPANY";

    private final ApplicationPortalRepository repository;

    public ApplicationPortalService(ApplicationPortalRepository repository) {
        this.repository = repository;
    }

    @Override
    public ApplicationResponse applyToPost(String email, UUID postId, ApplicationRequest request) {
        UUID applicantId = resolveUserId(email);
        String role = resolvePrimaryRole(applicantId);
        if (!ROLE_STUDENT.equals(role)) {
            throw new DomainException("Only student accounts can apply to recruitment posts");
        }

        ApplicationPortalRepository.PostApplyTarget postTarget = repository.findPostApplyTarget(postId)
                .orElseThrow(() -> new DomainException("Post not found"));

        if (!"APPROVED".equalsIgnoreCase(postTarget.approvalStatus())
                || !"OPEN".equalsIgnoreCase(postTarget.postStatus())) {
            throw new DomainException("Post is not open for applications");
        }

        if (postTarget.postType() == null || !postTarget.postType().startsWith("COMPANY_")) {
            throw new DomainException("Student can only apply to company recruitment posts");
        }

        if (repository.existsApplication(postId, applicantId)) {
            throw new IllegalStateException("You already applied to this post");
        }

        String requestCvUrl = request == null ? null : request.getCvUrl();
        String cvUrl = resolveCvUrl(applicantId, requestCvUrl);
        String message = request == null ? null : normalizeNullableText(request.getMessage());

        return repository.createApplication(postId, applicantId, message, cvUrl);
    }

    @Override
    public List<PendingApplicationResponse> getMyPendingApplications(String email) {
        UUID applicantId = resolveUserId(email);
        String role = resolvePrimaryRole(applicantId);
        if (!ROLE_STUDENT.equals(role)) {
            throw new DomainException("Only student accounts can view this list");
        }
        return repository.findPendingApplicationsByApplicant(applicantId);
    }

    @Override
    public List<PendingApplicantResponse> getApplicantsForMyCompanyPosts(String email, String status) {
        UUID companyId = resolveUserId(email);
        String role = resolvePrimaryRole(companyId);
        if (!ROLE_COMPANY.equals(role)) {
            throw new DomainException("Only company accounts can view this list");
        }
        return repository.findApplicantsByCompany(companyId, normalizeApplicantListStatus(status));
    }

    @Override
    public boolean updateApplicationStatusForMyCompanyPost(String email, UUID applicationId, String status) {
        UUID companyId = resolveUserId(email);
        String role = resolvePrimaryRole(companyId);
        if (!ROLE_COMPANY.equals(role)) {
            throw new DomainException("Only company accounts can update applicant status");
        }

        String normalizedStatus = normalizeCompanyApplicantStatus(status);
        boolean updated = repository.updateApplicationStatusForCompany(applicationId, companyId, normalizedStatus);
        if (!updated) {
            throw new DomainException("Application not found or already processed");
        }
        return true;
    }

    private UUID resolveUserId(String email) {
        if (!StringUtils.hasText(email)) {
            throw new DomainException("Authentication required");
        }

        return repository.findUserIdByEmail(email.trim())
                .orElseThrow(() -> new DomainException("Authenticated user is not found"));
    }

    private String resolvePrimaryRole(UUID userId) {
        return repository.findPrimaryRole(userId)
                .map((role) -> role.trim().toUpperCase())
                .orElse(ROLE_STUDENT);
    }

    private String resolveCvUrl(UUID applicantId, String requestCvUrl) {
        String normalizedRequestCv = normalizeNullableText(requestCvUrl);
        if (normalizedRequestCv != null) {
            return normalizedRequestCv;
        }

        return repository.findStudentDefaultCv(applicantId)
                .map(String::trim)
                .filter((value) -> !value.isBlank())
                .orElseThrow(() -> new DomainException("No default CV found. Please upload CV in profile first"));
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeCompanyApplicantStatus(String value) {
        if (!StringUtils.hasText(value)) {
            throw new DomainException("status is required");
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MARK", "MARKED", "REVIEW", "REVIEWED" -> "REVIEWED";
            case "REJECT", "REJECTED" -> "REJECTED";
            default -> throw new DomainException("Unsupported applicant status. Use REVIEWED or REJECTED.");
        };
    }

    private String normalizeApplicantListStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return "PENDING";
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PENDING", "REVIEWED", "REJECTED" -> normalized;
            default -> throw new DomainException("Unsupported applicant list status. Use PENDING, REVIEWED, or REJECTED.");
        };
    }
}
