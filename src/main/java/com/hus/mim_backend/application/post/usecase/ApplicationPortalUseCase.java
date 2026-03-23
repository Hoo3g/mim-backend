package com.hus.mim_backend.application.post.usecase;

import com.hus.mim_backend.application.post.dto.ApplicationRequest;
import com.hus.mim_backend.application.post.dto.ApplicationResponse;
import com.hus.mim_backend.application.post.dto.PendingApplicantResponse;
import com.hus.mim_backend.application.post.dto.PendingApplicationResponse;

import java.util.List;
import java.util.UUID;

public interface ApplicationPortalUseCase {
    ApplicationResponse applyToPost(String email, UUID postId, ApplicationRequest request);

    boolean cancelApplication(String email, UUID postId);

    List<PendingApplicationResponse> getMyPendingApplications(String email);

    List<PendingApplicantResponse> getApplicantsForMyCompanyPosts(String email, String status);

    default List<PendingApplicantResponse> getPendingApplicantsForMyCompanyPosts(String email) {
        return getApplicantsForMyCompanyPosts(email, "PENDING");
    }

    boolean deleteApplicationForMyCompanyPost(String email, UUID applicationId);

    boolean updateApplicationStatusForMyCompanyPost(String email, UUID applicationId, String status);
}
