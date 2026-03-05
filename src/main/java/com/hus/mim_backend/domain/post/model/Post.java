package com.hus.mim_backend.domain.post.model;

import com.hus.mim_backend.domain.shared.DomainException;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Post aggregate — Job posts and recruitment posts.
 * Contains domain business logic for state transitions.
 * Maps to: posts table
 */
@Setter
@Getter
public class Post {
    private UUID id;
    private UUID authorId;
    private String title;
    private String description;
    private String requirements;
    private String benefits;
    private String achievements;
    private PostType postType;
    private JobType jobType;
    private String studentCvUrl;
    private Map<String, Object> displayInfo;
    private String location;
    private String salaryRange;
    private String contactEmail;
    private String contactPhone;
    private List<String> tags;
    private PostStatus status;
    private ApprovalStatus approvalStatus;
    private UUID moderatorId;
    private String moderationComment;
    private int viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Post() {
    }

    // -------------------------------------------------------
    // Factory method
    // -------------------------------------------------------

    /**
     * Creates a new draft post. Business rule: all new posts start as PENDING
     * approval.
     */
    public static Post createNew(UUID authorId, String title, String description,
            PostType postType, JobType jobType) {
        if (title == null || title.isBlank())
            throw new DomainException("Post title must not be blank");
        if (description == null || description.isBlank())
            throw new DomainException("Post description must not be blank");

        Post post = new Post();
        post.id = UUID.randomUUID();
        post.authorId = authorId;
        post.title = title.trim();
        post.description = description.trim();
        post.postType = postType;
        post.jobType = jobType;
        post.status = PostStatus.OPEN;
        post.approvalStatus = ApprovalStatus.PENDING;
        post.viewCount = 0;
        post.createdAt = LocalDateTime.now();
        post.updatedAt = LocalDateTime.now();
        return post;
    }

    // -------------------------------------------------------
    // Domain methods
    // -------------------------------------------------------

    /**
     * Admin approves a post — moves it to APPROVED so it becomes publicly visible.
     */
    public void approve(UUID moderatorId, String comment) {
        if (this.approvalStatus == ApprovalStatus.APPROVED)
            return;
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.moderatorId = moderatorId;
        this.moderationComment = comment;
        this.updatedAt = LocalDateTime.now();
    }

    /** Admin rejects a post with an explanation. */
    public void reject(UUID moderatorId, String comment) {
        if (comment == null || comment.isBlank())
            throw new DomainException("Rejection comment is required");
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.moderatorId = moderatorId;
        this.moderationComment = comment;
        this.updatedAt = LocalDateTime.now();
    }

    /** Author closes the post (no longer accepting applications). */
    public void close() {
        if (this.status == PostStatus.CLOSED)
            return;
        this.status = PostStatus.CLOSED;
        this.updatedAt = LocalDateTime.now();
    }

    /** Reopen a closed post. */
    public void reopen() {
        this.status = PostStatus.OPEN;
        this.updatedAt = LocalDateTime.now();
    }

    /** Increment view counter. */
    public void incrementView() {
        this.viewCount++;
    }

    // -------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------

    // -------------------------------------------------------
    // Builder
    // -------------------------------------------------------

    public static PostBuilder builder() {
        return new PostBuilder();
    }

    public static class PostBuilder {
        private final Post post = new Post();

        public PostBuilder id(UUID id) {
            post.id = id;
            return this;
        }

        public PostBuilder authorId(UUID authorId) {
            post.authorId = authorId;
            return this;
        }

        public PostBuilder title(String title) {
            post.title = title;
            return this;
        }

        public PostBuilder description(String description) {
            post.description = description;
            return this;
        }

        public PostBuilder requirements(String requirements) {
            post.requirements = requirements;
            return this;
        }

        public PostBuilder benefits(String benefits) {
            post.benefits = benefits;
            return this;
        }

        public PostBuilder achievements(String achievements) {
            post.achievements = achievements;
            return this;
        }

        public PostBuilder postType(PostType postType) {
            post.postType = postType;
            return this;
        }

        public PostBuilder jobType(JobType jobType) {
            post.jobType = jobType;
            return this;
        }

        public PostBuilder studentCvUrl(String studentCvUrl) {
            post.studentCvUrl = studentCvUrl;
            return this;
        }

        public PostBuilder displayInfo(Map<String, Object> displayInfo) {
            post.displayInfo = displayInfo;
            return this;
        }

        public PostBuilder location(String location) {
            post.location = location;
            return this;
        }

        public PostBuilder salaryRange(String salaryRange) {
            post.salaryRange = salaryRange;
            return this;
        }

        public PostBuilder contactEmail(String contactEmail) {
            post.contactEmail = contactEmail;
            return this;
        }

        public PostBuilder contactPhone(String contactPhone) {
            post.contactPhone = contactPhone;
            return this;
        }

        public PostBuilder tags(List<String> tags) {
            post.tags = tags;
            return this;
        }

        public PostBuilder status(PostStatus status) {
            post.status = status;
            return this;
        }

        public PostBuilder approvalStatus(ApprovalStatus approvalStatus) {
            post.approvalStatus = approvalStatus;
            return this;
        }

        public PostBuilder moderatorId(UUID moderatorId) {
            post.moderatorId = moderatorId;
            return this;
        }

        public PostBuilder moderationComment(String moderationComment) {
            post.moderationComment = moderationComment;
            return this;
        }

        public PostBuilder viewCount(int viewCount) {
            post.viewCount = viewCount;
            return this;
        }

        public PostBuilder createdAt(LocalDateTime createdAt) {
            post.createdAt = createdAt;
            return this;
        }

        public PostBuilder updatedAt(LocalDateTime updatedAt) {
            post.updatedAt = updatedAt;
            return this;
        }

        public Post build() {
            return post;
        }
    }

    // -------------------------------------------------------
    // Enums
    // -------------------------------------------------------

    public enum PostType {
        STUDENT_SEEKING_JOB,
        STUDENT_SEEKING_INTERNSHIP,
        COMPANY_RECRUITING_JOB,
        COMPANY_RECRUITING_INTERNSHIP
    }

    public enum JobType {
        FULL_TIME,
        PART_TIME,
        CONTRACT,
        INTERNSHIP
    }

    public enum PostStatus {
        OPEN,
        CLOSED
    }

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
