package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.domain.post.model.Application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Application (job application) persistence operations
 */
public interface ApplicationRepository {
    Optional<Application> findById(UUID id);

    List<Application> findByPostId(UUID postId);

    List<Application> findByApplicantId(UUID applicantId);

    List<Application> findByStatus(String status);

    boolean existsByPostIdAndApplicantId(UUID postId, UUID applicantId);

    Application save(Application application);

    void deleteById(UUID id);
}
