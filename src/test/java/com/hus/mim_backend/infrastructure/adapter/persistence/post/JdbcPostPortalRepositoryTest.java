package com.hus.mim_backend.infrastructure.adapter.persistence.post;

import com.hus.mim_backend.application.post.dto.PublicPostResponse;
import com.hus.mim_backend.application.post.dto.UpsertRecruitmentPostRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JdbcPostPortalRepositoryTest {

    @Autowired
    private JdbcPostPortalRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createPostShouldPersistRowAndReturnReadablePost() {
        UUID authorId = UUID.randomUUID();
        String email = "post-test-" + UUID.randomUUID() + "@example.com";

        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, password, account_status)
                VALUES (?, ?, ?, ?)
                """,
                authorId,
                email,
                "noop-password",
                "APPROVED");

        UpsertRecruitmentPostRequest request = new UpsertRecruitmentPostRequest();
        request.setTitle("Repository create post test");
        request.setDescription("Ensures INSERT works without JDBC result-set mismatch.");
        request.setPostType("STUDENT_SEEKING_JOB");
        request.setJobType("FULL_TIME");
        request.setStatus("OPEN");
        request.setLocation("Ha Noi");
        request.setContactEmail(email);
        request.setTags(List.of("AI", "Data"));

        UUID postId = repository.createPost(authorId, request, null, "AI,Data", "PENDING");

        Optional<PublicPostResponse> created = repository.findPostByIdForAuthor(postId, authorId);

        assertTrue(created.isPresent());
        assertEquals("Repository create post test", created.get().getTitle());
        assertEquals("OPEN", created.get().getStatus());
        assertEquals("PENDING", created.get().getApprovalStatus());
        assertEquals(List.of("AI", "Data"), created.get().getTags());
    }

    @Test
    void createPostShouldAllowEmptyTags() {
        UUID authorId = UUID.randomUUID();
        String email = "post-empty-tags-" + UUID.randomUUID() + "@example.com";

        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, password, account_status)
                VALUES (?, ?, ?, ?)
                """,
                authorId,
                email,
                "noop-password",
                "APPROVED");

        UpsertRecruitmentPostRequest request = new UpsertRecruitmentPostRequest();
        request.setTitle("Post without tags");
        request.setDescription("Ensures null tags do not break PostgreSQL prepared statements.");
        request.setPostType("STUDENT_SEEKING_JOB");
        request.setJobType("FULL_TIME");
        request.setStatus("OPEN");
        request.setLocation("Ha Noi");
        request.setContactEmail(email);
        request.setTags(null);

        UUID postId = repository.createPost(authorId, request, null, null, "PENDING");

        Optional<PublicPostResponse> created = repository.findPostByIdForAuthor(postId, authorId);

        assertTrue(created.isPresent());
        assertEquals("Post without tags", created.get().getTitle());
        assertTrue(created.get().getTags() == null || created.get().getTags().isEmpty());
    }

    @Test
    void createPostShouldPersistExplicitApprovedStatus() {
        UUID authorId = insertUser("post-approved-" + UUID.randomUUID() + "@example.com");

        UpsertRecruitmentPostRequest request = new UpsertRecruitmentPostRequest();
        request.setTitle("Approved company post");
        request.setDescription("Published immediately for company accounts.");
        request.setPostType("COMPANY_RECRUITING_JOB");
        request.setJobType("FULL_TIME");
        request.setStatus("OPEN");

        UUID postId = repository.createPost(authorId, request, null, null, "APPROVED");

        Optional<PublicPostResponse> created = repository.findPostByIdForAuthor(postId, authorId);
        Optional<PublicPostResponse> visibleToGuest = repository.findPostByIdForViewer(postId, null);

        assertTrue(created.isPresent());
        assertEquals("APPROVED", created.get().getApprovalStatus());
        assertTrue(visibleToGuest.isPresent());
        assertEquals("APPROVED", visibleToGuest.get().getApprovalStatus());
    }

    @Test
    void updatePostByAuthorShouldKeepExplicitApprovedStatusAndClearRejectedComment() {
        UUID authorId = insertUser("post-update-" + UUID.randomUUID() + "@example.com");
        UUID postId = insertPost(authorId, "REJECTED", "Needs admin fixes");

        UpsertRecruitmentPostRequest request = new UpsertRecruitmentPostRequest();
        request.setTitle("Updated approved post");
        request.setDescription("Updated content should stay publicly available.");
        request.setPostType("COMPANY_RECRUITING_JOB");
        request.setJobType("FULL_TIME");
        request.setStatus("OPEN");

        boolean updated = repository.updatePostByAuthor(postId, authorId, request, null, null, "APPROVED");

        Optional<PublicPostResponse> refreshed = repository.findPostByIdForAuthor(postId, authorId);
        Optional<PublicPostResponse> visibleToGuest = repository.findPostByIdForViewer(postId, null);

        assertTrue(updated);
        assertTrue(refreshed.isPresent());
        assertEquals("APPROVED", refreshed.get().getApprovalStatus());
        assertEquals("Updated approved post", refreshed.get().getTitle());
        assertNull(refreshed.get().getModerationComment());
        assertTrue(visibleToGuest.isPresent());
        assertEquals("APPROVED", visibleToGuest.get().getApprovalStatus());
    }

    private UUID insertUser(String email) {
        UUID authorId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, password, account_status)
                VALUES (?, ?, ?, ?)
                """,
                authorId,
                email,
                "noop-password",
                "APPROVED");
        return authorId;
    }

    private UUID insertPost(UUID authorId, String approvalStatus, String moderationComment) {
        UUID postId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO posts (
                    id,
                    author_id,
                    title,
                    description,
                    requirements,
                    benefits,
                    achievements,
                    post_type,
                    job_type,
                    student_cv_url,
                    display_info,
                    location,
                    salary_range,
                    status,
                    approval_status,
                    contact_email,
                    contact_phone,
                    tags,
                    moderator_id,
                    moderation_comment,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(NULL AS jsonb), ?, ?, ?, ?, ?, ?, NULL, NULL, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                postId,
                authorId,
                "Existing rejected post",
                "Existing description",
                null,
                null,
                null,
                "COMPANY_RECRUITING_JOB",
                "FULL_TIME",
                null,
                "Ha Noi",
                null,
                "OPEN",
                approvalStatus,
                "owner@example.com",
                null,
                moderationComment);
        return postId;
    }
}
