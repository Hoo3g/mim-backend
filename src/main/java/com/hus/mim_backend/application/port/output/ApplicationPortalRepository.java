package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.application.post.dto.ApplicationResponse;
import com.hus.mim_backend.application.post.dto.PendingApplicantResponse;
import com.hus.mim_backend.application.post.dto.PendingApplicationResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationPortalRepository {
    Optional<UUID> findUserIdByEmail(String email);

    Optional<String> findPrimaryRole(UUID userId);

    Optional<PostApplyTarget> findPostApplyTarget(UUID postId);

    boolean existsApplication(UUID postId, UUID applicantId);

    ApplicationResponse createApplication(UUID postId, UUID applicantId, String message, String cvUrl);

    Optional<String> findStudentDefaultCv(UUID userId);

    List<PendingApplicationResponse> findPendingApplicationsByApplicant(UUID applicantId);

    List<PendingApplicantResponse> findPendingApplicantsByCompany(UUID companyId);

    record PostApplyTarget(UUID postId, UUID authorId, String postType, String approvalStatus, String postStatus) {}
}
