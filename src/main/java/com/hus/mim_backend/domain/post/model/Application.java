package com.hus.mim_backend.domain.post.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Application entity - Job applications to posts
 * Maps to: applications table
 */
public class Application {
    private UUID id;
    private UUID postId;
    private UUID applicantId;
    private ApplicationStatus status;
    private String message;
    private String cvUrl; // Specific CV version for this application
    private LocalDateTime createdAt;

    // TODO: Implement getters/setters
    // TODO: Implement builder pattern
    // TODO: Implement accept() domain logic
    // TODO: Implement reject() domain logic
    // TODO: Implement markAsReviewed() domain logic

    public enum ApplicationStatus {
        PENDING,
        REVIEWED,
        ACCEPTED,
        REJECTED
    }
}
