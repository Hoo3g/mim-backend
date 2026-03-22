package com.hus.mim_backend.infrastructure.adapter.persistence.post;

import com.hus.mim_backend.application.port.output.ApplicationPortalRepository;
import com.hus.mim_backend.application.port.output.UserRepository;
import com.hus.mim_backend.application.post.dto.ApplicationResponse;
import com.hus.mim_backend.application.post.dto.PendingApplicantResponse;
import com.hus.mim_backend.application.post.dto.PendingApplicationResponse;
import com.hus.mim_backend.infrastructure.adapter.persistence.JdbcMappingUtils;
import com.hus.mim_backend.infrastructure.adapter.persistence.PersistenceSqlFragments;
import com.hus.mim_backend.shared.constants.RoleNames;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JdbcApplicationPortalRepository implements ApplicationPortalRepository {

    /**
     * Resolves a user's primary role by priority: ADMIN > LECTURER > COMPANY > STUDENT.
     */
    private static final String SELECT_PRIMARY_ROLE_SQL = """
            SELECT COALESCE(r.name, '%s')
            FROM users u
            LEFT JOIN user_roles ur ON ur.user_id = u.id
            LEFT JOIN roles r ON r.id = ur.role_id
            WHERE u.id = ?
            ORDER BY CASE r.name
                WHEN 'ADMIN'    THEN 1
                WHEN 'LECTURER' THEN 2
                WHEN 'COMPANY'  THEN 3
                WHEN 'STUDENT'  THEN 4
                ELSE 99
            END
            LIMIT 1
            """.formatted(RoleNames.STUDENT);

    private static final String SELECT_POST_TARGET_SQL = """
            SELECT id, author_id, post_type, approval_status, status
            FROM posts
            WHERE id = ?
            """;

    private static final String EXISTS_APPLICATION_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM applications
                WHERE post_id = ? AND applicant_id = ?
            )
            """;

    private static final String INSERT_APPLICATION_SQL = """
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
            RETURNING id, post_id, status, message, cv_url, created_at
            """;

    private static final String SELECT_STUDENT_DEFAULT_CV_SQL = """
            SELECT cv_url FROM students WHERE id = ?
            """;

    private static final String SELECT_PENDING_APPLICATIONS_SQL = """
            SELECT a.id AS application_id,
                   p.id AS post_id,
                   p.title AS post_title,
                   %s AS company_name,
                   p.post_type,
                   p.location,
                   a.status,
                   a.created_at
            FROM applications a
            JOIN posts p ON p.id = a.post_id
            LEFT JOIN users u ON u.id = p.author_id
            LEFT JOIN companies c ON c.id = p.author_id
            LEFT JOIN students s ON s.id = p.author_id
            LEFT JOIN lecturers l ON l.id = p.author_id
            WHERE a.applicant_id = ?
              AND a.status = 'PENDING'
            ORDER BY a.created_at DESC
            """.formatted(PersistenceSqlFragments.AUTHOR_NAME_SQL);

    private static final String SELECT_APPLICANTS_BY_COMPANY_SQL = """
            SELECT a.id AS application_id,
                   p.id AS post_id,
                   p.title AS post_title,
                   a.applicant_id,
                   applicant_post.id AS applicant_post_id,
                   a.status,
                   COALESCE(
                       NULLIF(TRIM(COALESCE(s.first_name, '') || ' ' || COALESCE(s.last_name, '')), ''),
                       NULLIF(TRIM(COALESCE(l.first_name, '') || ' ' || COALESCE(l.last_name, '')), ''),
                       NULLIF(c.name, ''),
                       SPLIT_PART(COALESCE(u.email, ''), '@', 1),
                       'Unknown'
                   ) AS applicant_name,
                   a.message,
                   a.cv_url,
                   a.created_at
            FROM applications a
            JOIN posts p ON p.id = a.post_id
            LEFT JOIN users u ON u.id = a.applicant_id
            LEFT JOIN students s ON s.id = a.applicant_id
            LEFT JOIN lecturers l ON l.id = a.applicant_id
            LEFT JOIN companies c ON c.id = a.applicant_id
            LEFT JOIN LATERAL (
                SELECT candidate.id
                FROM posts candidate
                WHERE candidate.author_id = a.applicant_id
                  AND candidate.post_type LIKE 'STUDENT_%%'
                  AND candidate.approval_status = 'APPROVED'
                  AND candidate.status = 'OPEN'
                ORDER BY candidate.updated_at DESC NULLS LAST, candidate.created_at DESC
                LIMIT 1
            ) applicant_post ON TRUE
            WHERE p.author_id = ?
              AND p.post_type LIKE 'COMPANY_%%'
              AND a.status = ?
            ORDER BY a.created_at DESC
            """;

    private static final String UPDATE_APPLICATION_STATUS_BY_COMPANY_SQL = """
            UPDATE applications a
            SET status = ?
            FROM posts p
            WHERE a.id = ?
              AND p.id = a.post_id
              AND p.author_id = ?
              AND p.post_type LIKE 'COMPANY_%%'
              AND a.status = 'PENDING'
            """;

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    public JdbcApplicationPortalRepository(JdbcTemplate jdbcTemplate, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UUID> findUserIdByEmail(String email) {
        return userRepository.findIdByEmail(email);
    }

    @Override
    public Optional<String> findPrimaryRole(UUID userId) {
        List<String> rows = jdbcTemplate.query(
                SELECT_PRIMARY_ROLE_SQL,
                (rs, rowNum) -> rs.getString(1),
                userId);
        return rows.isEmpty() ? Optional.empty() : Optional.ofNullable(rows.getFirst());
    }

    @Override
    public Optional<PostApplyTarget> findPostApplyTarget(UUID postId) {
        List<PostApplyTarget> rows = jdbcTemplate.query(SELECT_POST_TARGET_SQL,
                (rs, rowNum) -> new PostApplyTarget(
                        rs.getObject("id", UUID.class),
                        rs.getObject("author_id", UUID.class),
                        rs.getString("post_type"),
                        rs.getString("approval_status"),
                        rs.getString("status")),
                postId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    @Override
    public boolean existsApplication(UUID postId, UUID applicantId) {
        Boolean result = jdbcTemplate.queryForObject(EXISTS_APPLICATION_SQL, Boolean.class, postId, applicantId);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public ApplicationResponse createApplication(UUID postId, UUID applicantId, String message, String cvUrl) {
        UUID applicationId = UUID.randomUUID();
        return jdbcTemplate.queryForObject(INSERT_APPLICATION_SQL, (rs, rowNum) -> {
            ApplicationResponse response = new ApplicationResponse();
            response.setId(rs.getObject("id", UUID.class));
            response.setPostId(rs.getObject("post_id", UUID.class));
            response.setApplicantId(applicantId);
            response.setStatus(rs.getString("status"));
            response.setMessage(rs.getString("message"));
            response.setCvUrl(rs.getString("cv_url"));
            response.setCreatedAt(JdbcMappingUtils.toLocalDateTime(rs.getTimestamp("created_at")));
            return response;
        }, applicationId, postId, applicantId, message, cvUrl);
    }

    @Override
    public Optional<String> findStudentDefaultCv(UUID userId) {
        List<String> rows = jdbcTemplate.query(
                SELECT_STUDENT_DEFAULT_CV_SQL,
                (rs, rowNum) -> rs.getString("cv_url"),
                userId);
        return rows.isEmpty() ? Optional.empty() : Optional.ofNullable(rows.getFirst());
    }

    @Override
    public List<PendingApplicationResponse> findPendingApplicationsByApplicant(UUID applicantId) {
        return jdbcTemplate.query(SELECT_PENDING_APPLICATIONS_SQL, (rs, rowNum) -> {
            PendingApplicationResponse item = new PendingApplicationResponse();
            item.setApplicationId(rs.getObject("application_id", UUID.class));
            item.setPostId(rs.getObject("post_id", UUID.class));
            item.setPostTitle(rs.getString("post_title"));
            item.setCompanyName(rs.getString("company_name"));
            item.setPostType(rs.getString("post_type"));
            item.setLocation(rs.getString("location"));
            item.setStatus(rs.getString("status"));
            item.setAppliedAt(JdbcMappingUtils.toLocalDateTime(rs.getTimestamp("created_at")));
            return item;
        }, applicantId);
    }

    @Override
    public List<PendingApplicantResponse> findApplicantsByCompany(UUID companyId, String status) {
        return jdbcTemplate.query(SELECT_APPLICANTS_BY_COMPANY_SQL, (rs, rowNum) -> {
            PendingApplicantResponse item = new PendingApplicantResponse();
            item.setApplicationId(rs.getObject("application_id", UUID.class));
            item.setPostId(rs.getObject("post_id", UUID.class));
            item.setPostTitle(rs.getString("post_title"));
            item.setApplicantId(rs.getObject("applicant_id", UUID.class));
            item.setApplicantPostId(rs.getObject("applicant_post_id", UUID.class));
            item.setStatus(rs.getString("status"));
            item.setApplicantName(rs.getString("applicant_name"));
            item.setMessage(rs.getString("message"));
            item.setCvUrl(rs.getString("cv_url"));
            item.setAppliedAt(JdbcMappingUtils.toLocalDateTime(rs.getTimestamp("created_at")));
            return item;
        }, companyId, status);
    }

    @Override
    public boolean updateApplicationStatusForCompany(UUID applicationId, UUID companyId, String status) {
        return jdbcTemplate.update(UPDATE_APPLICATION_STATUS_BY_COMPANY_SQL, status, applicationId, companyId) > 0;
    }
}
