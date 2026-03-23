package com.hus.mim_backend.infrastructure.adapter.persistence.post;

import com.hus.mim_backend.application.post.dto.PublicPostResponse;
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
class JdbcPublicPostRepositoryTest {

    @Autowired
    private JdbcPublicPostRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findApprovedPostsShouldMatchConfiguredTags() {
        UUID authorId = insertUser("company-public-" + UUID.randomUUID() + "@example.com");
        UUID postId = insertApprovedCompanyPost(authorId, "BACKEND,FULLSTACK");

        List<PublicPostResponse> backendOnly = repository.findApprovedPosts("", "company", List.of("backend"));
        List<PublicPostResponse> backendAndFullstack = repository.findApprovedPosts(
                "",
                "company",
                List.of("backend", "fullstack"));

        assertEquals(List.of(postId), backendOnly.stream().map(PublicPostResponse::getId).toList());
        assertEquals(List.of(postId), backendAndFullstack.stream().map(PublicPostResponse::getId).toList());
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

    private UUID insertApprovedCompanyPost(UUID authorId, String tagsCsv) {
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
                VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(NULL AS jsonb), ?, ?, ?, ?, ?, ?,
                    string_to_array(?, ','),
                    NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """,
                postId,
                authorId,
                "Backend Fullstack Position",
                "Hiring for backend and fullstack responsibilities.",
                null,
                null,
                null,
                "COMPANY_RECRUITING_JOB",
                "FULL_TIME",
                null,
                "Ha Noi",
                "12M-18M",
                "OPEN",
                "APPROVED",
                "owner@example.com",
                null,
                tagsCsv);
        return postId;
    }
}
