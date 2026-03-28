package com.hus.mim_backend.infrastructure.adapter.persistence.moderation;

import com.hus.mim_backend.application.moderation.dto.ModerationPaperResponse;
import com.hus.mim_backend.application.moderation.dto.ModerationPostResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JdbcAdminModerationRepositoryTest {

    @Autowired
    private JdbcAdminModerationRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void updatePostModerationShouldSucceedOnlyForPendingPosts() {
        UUID authorId = insertUser("post-author-" + UUID.randomUUID() + "@example.com");
        UUID moderatorId = insertUser("post-moderator-" + UUID.randomUUID() + "@example.com");
        UUID pendingPostId = insertPost(authorId, "PENDING");
        UUID approvedPostId = insertPost(authorId, "APPROVED");

        int updatedPending = repository.updatePostModeration(pendingPostId, "APPROVED", moderatorId, null);
        int updatedApproved = repository.updatePostModeration(approvedPostId, "REJECTED", moderatorId, "stale");

        assertEquals(1, updatedPending);
        assertEquals("APPROVED", jdbcTemplate.queryForObject(
                "SELECT approval_status FROM posts WHERE id = ?",
                String.class,
                pendingPostId));
        assertEquals(0, updatedApproved);
    }

    @Test
    void updatePostModerationShouldReturnZeroWhenPostWasDeleted() {
        UUID authorId = insertUser("deleted-post-author-" + UUID.randomUUID() + "@example.com");
        UUID moderatorId = insertUser("deleted-post-moderator-" + UUID.randomUUID() + "@example.com");
        UUID postId = insertPost(authorId, "PENDING");

        jdbcTemplate.update("DELETE FROM posts WHERE id = ?", postId);

        int updated = repository.updatePostModeration(postId, "APPROVED", moderatorId, null);

        assertEquals(0, updated);
    }

    @Test
    void updatePaperModerationShouldSucceedOnlyForPendingPapers() {
        UUID moderatorId = insertUser("paper-moderator-" + UUID.randomUUID() + "@example.com");
        UUID pendingPaperId = insertPaper("PENDING");
        UUID approvedPaperId = insertPaper("APPROVED");

        int updatedPending = repository.updatePaperModeration(pendingPaperId, "APPROVED", moderatorId, null);
        int updatedApproved = repository.updatePaperModeration(approvedPaperId, "REJECTED", moderatorId, "stale");

        assertEquals(1, updatedPending);
        assertEquals("APPROVED", jdbcTemplate.queryForObject(
                "SELECT approval_status FROM research_papers WHERE id = ?",
                String.class,
                pendingPaperId));
        assertEquals(0, updatedApproved);
    }

    @Test
    void updatePaperModerationShouldReturnZeroWhenPaperWasDeleted() {
        UUID moderatorId = insertUser("deleted-paper-moderator-" + UUID.randomUUID() + "@example.com");
        UUID paperId = insertPaper("PENDING");

        jdbcTemplate.update("DELETE FROM research_papers WHERE id = ?", paperId);

        int updated = repository.updatePaperModeration(paperId, "APPROVED", moderatorId, null);

        assertEquals(0, updated);
    }

    @Test
    void deletePostByIdShouldDeleteExistingPost() {
        UUID authorId = insertUser("post-delete-author-" + UUID.randomUUID() + "@example.com");
        UUID postId = insertPost(authorId, "APPROVED");

        int deleted = repository.deletePostById(postId);

        assertEquals(1, deleted);
        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM posts WHERE id = ?",
                Integer.class,
                postId);
        assertEquals(0, remaining);
    }

    @Test
    void deletePaperByIdShouldDeleteExistingPaper() {
        UUID paperId = insertPaper("REJECTED");

        int deleted = repository.deletePaperById(paperId);

        assertEquals(1, deleted);
        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM research_papers WHERE id = ?",
                Integer.class,
                paperId);
        assertEquals(0, remaining);
    }

    @Test
    void findPostsByStatusShouldSupportKeywordAndLimit() {
        UUID authorId = insertUser("post-search-author-" + UUID.randomUUID() + "@example.com");
        insertPost(authorId, "APPROVED", "Backend Java Position", "Need Spring Boot skill");
        insertPost(authorId, "APPROVED", "Frontend Angular Position", "Need Angular skill");

        List<ModerationPostResponse> result = repository.findPostsByStatus("APPROVED", "java", 1, 0);
        long total = repository.countPostsByStatus("APPROVED", "java");

        assertEquals(1, result.size());
        assertEquals(1L, total);
        assertEquals("Backend Java Position", result.get(0).getTitle());
    }

    @Test
    void findPapersByStatusShouldSupportKeywordAndLimit() {
        insertPaper("APPROVED", "AI Optimization", "Neural optimization methods");
        insertPaper("APPROVED", "Database Systems", "Postgres indexing");

        List<ModerationPaperResponse> result = repository.findPapersByStatus("APPROVED", "neural", 1, 0);
        long total = repository.countPapersByStatus("APPROVED", "neural");

        assertEquals(1, result.size());
        assertEquals(1L, total);
        assertEquals("AI Optimization", result.get(0).getTitle());
    }

    private UUID insertUser(String email) {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, password, account_status)
                VALUES (?, ?, ?, ?)
                """,
                userId,
                email,
                "noop-password",
                "APPROVED");
        return userId;
    }

    private UUID insertPost(UUID authorId, String approvalStatus) {
        return insertPost(authorId, approvalStatus, "Moderation test post", "Moderation test description");
    }

    private UUID insertPost(UUID authorId, String approvalStatus, String title, String description) {
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
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(NULL AS jsonb), ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                postId,
                authorId,
                title,
                description,
                null,
                null,
                null,
                "STUDENT_SEEKING_JOB",
                "FULL_TIME",
                null,
                "Ha Noi",
                null,
                "OPEN",
                approvalStatus,
                "owner@example.com",
                null);
        return postId;
    }

    private UUID insertPaper(String approvalStatus) {
        return insertPaper(approvalStatus, "Moderation test paper", "Abstract");
    }

    private UUID insertPaper(String approvalStatus, String title, String paperAbstract) {
        UUID paperId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO research_papers (
                    id,
                    title,
                    abstract,
                    pdf_url,
                    publication_year,
                    journal_conference,
                    research_area,
                    category,
                    approval_status,
                    moderator_id,
                    moderation_comment,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                paperId,
                title,
                paperAbstract,
                "https://example.com/paper.pdf",
                2026,
                "Conference",
                "AI",
                "AI",
                approvalStatus);
        return paperId;
    }
}
