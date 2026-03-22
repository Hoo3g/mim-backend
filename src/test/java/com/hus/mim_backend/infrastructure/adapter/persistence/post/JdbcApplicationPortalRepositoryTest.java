package com.hus.mim_backend.infrastructure.adapter.persistence.post;

import com.hus.mim_backend.application.post.dto.PendingApplicantResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JdbcApplicationPortalRepositoryTest {

    @Autowired
    private JdbcApplicationPortalRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findApplicantsByCompanyShouldIncludeApplicantStudentPostId() {
        UUID companyId = insertUser("company-stat-" + UUID.randomUUID() + "@example.com");
        UUID studentId = insertUser("student-stat-" + UUID.randomUUID() + "@example.com");
        UUID companyPostId = insertPost(companyId, "COMPANY_RECRUITING_JOB", "APPROVED", "OPEN");
        UUID applicantPostId = insertPost(studentId, "STUDENT_SEEKING_JOB", "APPROVED", "OPEN");
        insertApplication(companyPostId, studentId);

        List<PendingApplicantResponse> applicants = repository.findApplicantsByCompany(companyId, "PENDING");

        assertEquals(1, applicants.size());
        assertEquals(applicantPostId, applicants.getFirst().getApplicantPostId());
        assertEquals("PENDING", applicants.getFirst().getStatus());
    }

    @Test
    void findApplicantsByCompanyShouldReturnNullApplicantPostIdWhenStudentHasNoPublicPost() {
        UUID companyId = insertUser("company-null-stat-" + UUID.randomUUID() + "@example.com");
        UUID studentId = insertUser("student-null-stat-" + UUID.randomUUID() + "@example.com");
        UUID companyPostId = insertPost(companyId, "COMPANY_RECRUITING_JOB", "APPROVED", "OPEN");
        insertPost(studentId, "STUDENT_SEEKING_JOB", "PENDING", "OPEN");
        insertApplication(companyPostId, studentId);

        List<PendingApplicantResponse> applicants = repository.findApplicantsByCompany(companyId, "PENDING");

        assertEquals(1, applicants.size());
        assertNull(applicants.getFirst().getApplicantPostId());
    }

    @Test
    void findApplicantsByCompanyShouldFilterReviewedApplicants() {
        UUID companyId = insertUser("company-reviewed-stat-" + UUID.randomUUID() + "@example.com");
        UUID studentId = insertUser("student-reviewed-stat-" + UUID.randomUUID() + "@example.com");
        UUID companyPostId = insertPost(companyId, "COMPANY_RECRUITING_JOB", "APPROVED", "OPEN");
        UUID applicationId = insertApplication(companyPostId, studentId);

        repository.updateApplicationStatusForCompany(applicationId, companyId, "REVIEWED");

        List<PendingApplicantResponse> reviewed = repository.findApplicantsByCompany(companyId, "REVIEWED");
        List<PendingApplicantResponse> pending = repository.findApplicantsByCompany(companyId, "PENDING");

        assertEquals(1, reviewed.size());
        assertEquals(applicationId, reviewed.getFirst().getApplicationId());
        assertEquals(0, pending.size());
    }

    @Test
    void updateApplicationStatusForCompanyShouldOnlyProcessPendingApplicationOwnedByCompany() {
        UUID companyId = insertUser("company-update-stat-" + UUID.randomUUID() + "@example.com");
        UUID studentId = insertUser("student-update-stat-" + UUID.randomUUID() + "@example.com");
        UUID companyPostId = insertPost(companyId, "COMPANY_RECRUITING_JOB", "APPROVED", "OPEN");
        UUID applicationId = insertApplication(companyPostId, studentId);

        boolean updated = repository.updateApplicationStatusForCompany(applicationId, companyId, "REVIEWED");

        assertTrue(updated);
        assertEquals("REVIEWED", jdbcTemplate.queryForObject(
                "SELECT status FROM applications WHERE id = ?",
                String.class,
                applicationId));
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

    private UUID insertPost(UUID authorId, String postType, String approvalStatus, String status) {
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
                "Statistics post",
                "Statistics description",
                null,
                null,
                null,
                postType,
                "FULL_TIME",
                null,
                "Ha Noi",
                null,
                status,
                approvalStatus,
                "owner@example.com",
                null);
        return postId;
    }

    private UUID insertApplication(UUID postId, UUID applicantId) {
        UUID applicationId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO applications (
                    id,
                    post_id,
                    applicant_id,
                    status,
                    message,
                    cv_url,
                    created_at
                )
                VALUES (?, ?, ?, 'PENDING', ?, ?, CURRENT_TIMESTAMP)
                """,
                applicationId,
                postId,
                applicantId,
                "Interested",
                "https://example.com/cv.pdf");
        return applicationId;
    }
}
