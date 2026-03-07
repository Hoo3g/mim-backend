package com.hus.mim_backend.application.post.usecase;

import com.hus.mim_backend.application.post.dto.ApplicationRequest;
import com.hus.mim_backend.application.post.dto.ApplicationResponse;
import com.hus.mim_backend.application.post.dto.PendingApplicantResponse;
import com.hus.mim_backend.application.post.dto.PendingApplicationResponse;

import java.util.List;
import java.util.UUID;

public interface ApplicationPortalUseCase {
    ApplicationResponse applyToPost(String email, UUID postId, ApplicationRequest request);

    List<PendingApplicationResponse> getMyPendingApplications(String email);

    List<PendingApplicantResponse> getPendingApplicantsForMyCompanyPosts(String email);
}
