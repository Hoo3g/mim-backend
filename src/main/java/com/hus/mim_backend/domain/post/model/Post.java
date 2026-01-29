package com.hus.mim_backend.domain.post.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Post aggregate - Job posts and recruitment posts
 * Maps to: posts table
 */
public class Post {
    private UUID id;
    private UUID authorId;
    private String title;
    private String description;
    private String requirements;
    private String benefits;
    private String achievements;
    private PostType postType; // STUDENT_SEEKING_JOB, COMPANY_RECRUITING_JOB
    private JobType jobType; // FULL_TIME, PART_TIME, INTERNSHIP
    private String studentCvUrl; // Specific PDF CV for this post if by student
    private Map<String, Object> displayInfo; // JSONB - Flexible metadata for web display
    private String location;
    private String salaryRange;
    private String contactEmail;
    private String contactPhone;
    private List<String> tags;
    private PostStatus status; // OPEN, CLOSED
    private ApprovalStatus approvalStatus; // PENDING, APPROVED, REJECTED
    private UUID moderatorId;
    private String moderationComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // TODO: Implement getters/setters
    // TODO: Implement builder pattern
    // TODO: Implement publish() domain logic
    // TODO: Implement close() domain logic
    // TODO: Implement approve(moderatorId, comment) domain logic
    // TODO: Implement reject(moderatorId, comment) domain logic

    public enum PostType {
        STUDENT_SEEKING_JOB,
        COMPANY_RECRUITING_JOB
        // TODO: Add more types as needed
    }

    public enum JobType {
        FULL_TIME,
        PART_TIME,
        INTERNSHIP
        // TODO: Add more types as needed
    }

    public enum PostStatus {
        OPEN,
        CLOSED
    }

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED,
    }
}
