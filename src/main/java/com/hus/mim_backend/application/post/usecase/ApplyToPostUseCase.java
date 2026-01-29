package com.hus.mim_backend.application.post.usecase;

import com.hus.mim_backend.application.post.dto.ApplicationRequest;
import com.hus.mim_backend.application.post.dto.ApplicationResponse;
import java.util.List;
import java.util.UUID;

/**
 * Input port for handling post applications
 */
public interface ApplyToPostUseCase {
    ApplicationResponse apply(UUID applicantId, UUID postId, ApplicationRequest request);

    void updateApplicationStatus(UUID applicationId, String status);

    List<ApplicationResponse> getApplicationsForPost(UUID postId);

    List<ApplicationResponse> getMyApplications(UUID applicantId);
}
