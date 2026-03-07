package com.hus.mim_backend.infrastructure.adapter.persistence.post;

import com.hus.mim_backend.application.port.output.ApplicationPortalRepository;
import com.hus.mim_backend.application.post.dto.ApplicationResponse;
import com.hus.mim_backend.application.post.dto.PendingApplicantResponse;
import com.hus.mim_backend.application.post.dto.PendingApplicationResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JdbcApplicationPortalRepository implements ApplicationPortalRepository {
    private static final String SELECT_USER_ID_BY_EMAIL_SQL = """
            SELECT id FROM users WHERE email = ?
            """;

    private static final String SELECT_PRIMARY_ROLE_SQL = """
            SELECT COALESCE(r.name, 'STUDENT')
            FROM users u
            LEFT JOIN user_roles ur ON ur.user_id = u.id
            LEFT JOIN roles r ON r.id = ur.role_id
            WHERE u.id = ?
            ORDER BY CASE r.name
                WHEN 'ADMIN' THEN 1
                WHEN 'LECTURER' THEN 2
                WHEN 'COMPANY' THEN 3
                WHEN 'STUDENT' THEN 4
                ELSE 99
            END
            LIMIT 1
            """;

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
            VALUES (uuid_generate_v4(), ?, ?, 'PENDING', ?, ?, CURRENT_TIMESTAMP)
            RETURNING id, post_id, status, message, cv_url, created_at
            """;

    private static final String SELECT_STUDENT_DEFAULT_CV_SQL = """
            SELECT cv_url
            FROM students
            WHERE id = ?
            """;

    private static final String SELECT_PENDING_APPLICATIONS_SQL = """
            SELECT a.id AS application_id,
                   p.id AS post_id,
                   p.title AS post_title,
                   COALESCE(
                       NULLIF(c.name, ''),
                       NULLIF(TRIM(COALESCE(s.first_name, '') || ' ' || COALESCE(s.last_name, '')), ''),
                       NULLIF(TRIM(COALESCE(l.first_name, '') || ' ' || COALESCE(l.last_name, '')), ''),
                       SPLIT_PART(COALESCE(u.email, ''), '@', 1),
                       'Unknown'
                   ) AS company_name,
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
            """;

    private static final String SELECT_PENDING_APPLICANTS_BY_COMPANY_SQL = """
            SELECT a.id AS application_id,
                   p.id AS post_id,
                   p.title AS post_title,
                   a.applicant_id,
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
            WHERE p.author_id = ?
              AND p.post_type LIKE 'COMPANY_%'
              AND a.status = 'PENDING'
            ORDER BY a.created_at DESC
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcApplicationPortalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UUID> findUserIdByEmail(String email) {
        List<UUID> ids = jdbcTemplate.query(SELECT_USER_ID_BY_EMAIL_SQL,
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                email);
        if (ids.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ids.getFirst());
    }

    @Override
    public Optional<String> findPrimaryRole(UUID userId) {
        List<String> rows = jdbcTemplate.query(SELECT_PRIMARY_ROLE_SQL,
                (rs, rowNum) -> rs.getString(1),
                userId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(rows.getFirst());
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
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    @Override
    public boolean existsApplication(UUID postId, UUID applicantId) {
        Boolean result = jdbcTemplate.queryForObject(EXISTS_APPLICATION_SQL, Boolean.class, postId, applicantId);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public ApplicationResponse createApplication(UUID postId, UUID applicantId, String message, String cvUrl) {
        return jdbcTemplate.queryForObject(INSERT_APPLICATION_SQL, (rs, rowNum) -> {
            ApplicationResponse response = new ApplicationResponse();
            response.setId(rs.getObject("id", UUID.class));
            response.setPostId(rs.getObject("post_id", UUID.class));
            response.setApplicantId(applicantId);
            response.setStatus(rs.getString("status"));
            response.setMessage(rs.getString("message"));
            response.setCvUrl(rs.getString("cv_url"));
            response.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
            return response;
        }, postId, applicantId, message, cvUrl);
    }

    @Override
    public Optional<String> findStudentDefaultCv(UUID userId) {
        List<String> rows = jdbcTemplate.query(SELECT_STUDENT_DEFAULT_CV_SQL,
                (rs, rowNum) -> rs.getString("cv_url"),
                userId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(rows.getFirst());
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
            item.setAppliedAt(toLocalDateTime(rs.getTimestamp("created_at")));
            return item;
        }, applicantId);
    }

    @Override
    public List<PendingApplicantResponse> findPendingApplicantsByCompany(UUID companyId) {
        return jdbcTemplate.query(SELECT_PENDING_APPLICANTS_BY_COMPANY_SQL, (rs, rowNum) -> {
            PendingApplicantResponse item = new PendingApplicantResponse();
            item.setApplicationId(rs.getObject("application_id", UUID.class));
            item.setPostId(rs.getObject("post_id", UUID.class));
            item.setPostTitle(rs.getString("post_title"));
            item.setApplicantId(rs.getObject("applicant_id", UUID.class));
            item.setApplicantName(rs.getString("applicant_name"));
            item.setMessage(rs.getString("message"));
            item.setCvUrl(rs.getString("cv_url"));
            item.setAppliedAt(toLocalDateTime(rs.getTimestamp("created_at")));
            return item;
        }, companyId);
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime();
    }
}
