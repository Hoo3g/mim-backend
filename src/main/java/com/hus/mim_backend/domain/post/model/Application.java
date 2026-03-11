package com.hus.mim_backend.domain.post.model;

import com.hus.mim_backend.domain.shared.DomainException;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Application entity - Job applications to posts
 * Maps to: applications table
 */
@Getter
@Setter
public class Application {
    private UUID id;
    private UUID postId;
    private UUID applicantId;
    private ApplicationStatus status;
    private String message;
    private String cvUrl; // Specific CV version for this application
    private LocalDateTime createdAt;

    public Application() {
    }

    public static ApplicationBuilder builder() {
        return new ApplicationBuilder();
    }

    public void accept() {
        if (status == ApplicationStatus.ACCEPTED) {
            return;
        }
        if (status == ApplicationStatus.REJECTED) {
            throw new DomainException("Rejected application cannot be accepted");
        }
        this.status = ApplicationStatus.ACCEPTED;
    }

    public void reject() {
        if (status == ApplicationStatus.REJECTED) {
            return;
        }
        if (status == ApplicationStatus.ACCEPTED) {
            throw new DomainException("Accepted application cannot be rejected");
        }
        this.status = ApplicationStatus.REJECTED;
    }

    public void markAsReviewed() {
        if (status == null || status == ApplicationStatus.PENDING) {
            this.status = ApplicationStatus.REVIEWED;
        }
    }

    public enum ApplicationStatus {
        PENDING,
        REVIEWED,
        ACCEPTED,
        REJECTED
    }

    public static class ApplicationBuilder {
        private final Application application = new Application();

        public ApplicationBuilder id(UUID id) {
            application.id = id;
            return this;
        }

        public ApplicationBuilder postId(UUID postId) {
            application.postId = postId;
            return this;
        }

        public ApplicationBuilder applicantId(UUID applicantId) {
            application.applicantId = applicantId;
            return this;
        }

        public ApplicationBuilder status(ApplicationStatus status) {
            application.status = status;
            return this;
        }

        public ApplicationBuilder message(String message) {
            application.message = message;
            return this;
        }

        public ApplicationBuilder cvUrl(String cvUrl) {
            application.cvUrl = cvUrl;
            return this;
        }

        public ApplicationBuilder createdAt(LocalDateTime createdAt) {
            application.createdAt = createdAt;
            return this;
        }

        public Application build() {
            return application;
        }
    }
}
