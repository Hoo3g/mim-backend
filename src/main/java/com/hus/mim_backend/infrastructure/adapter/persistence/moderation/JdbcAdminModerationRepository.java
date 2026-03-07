package com.hus.mim_backend.infrastructure.adapter.persistence.moderation;

import com.hus.mim_backend.application.moderation.dto.ModerationPaperResponse;
import com.hus.mim_backend.application.moderation.dto.ModerationPostResponse;
import com.hus.mim_backend.application.port.output.AdminModerationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC adapter for admin moderation queue/actions.
 */
@Component
public class JdbcAdminModerationRepository implements AdminModerationRepository {
    private static final String SELECT_PENDING_POSTS_SQL = """
            SELECT p.id,
                   p.title,
                   p.description,
                   p.approval_status,
                   p.created_at,
                   COALESCE(
                     NULLIF(c.name, ''),
                     NULLIF(TRIM(COALESCE(s.first_name, '') || ' ' || COALESCE(s.last_name, '')), ''),
                     NULLIF(TRIM(COALESCE(l.first_name, '') || ' ' || COALESCE(l.last_name, '')), ''),
                     SPLIT_PART(COALESCE(u.email, ''), '@', 1),
                     'Unknown'
                   ) AS author_name
            FROM posts p
            LEFT JOIN users u ON u.id = p.author_id
            LEFT JOIN companies c ON c.id = p.author_id
            LEFT JOIN students s ON s.id = p.author_id
            LEFT JOIN lecturers l ON l.id = p.author_id
            WHERE COALESCE(p.approval_status, 'PENDING') = ?
            ORDER BY p.created_at DESC
            """;

    private static final String SELECT_PENDING_PAPERS_SQL = """
            SELECT rp.id,
                   rp.title,
                   rp.category,
                   COALESCE(rp.approval_status, 'PENDING') AS approval_status,
                   rp.created_at,
                   COALESCE(author_info.author_name, 'Unknown') AS author_name
            FROM research_papers rp
            LEFT JOIN LATERAL (
                SELECT COALESCE(
                         NULLIF(TRIM(COALESCE(s.first_name, '') || ' ' || COALESCE(s.last_name, '')), ''),
                         NULLIF(TRIM(COALESCE(l.first_name, '') || ' ' || COALESCE(l.last_name, '')), ''),
                         SPLIT_PART(COALESCE(us.email, ul.email, ''), '@', 1),
                         'Unknown'
                       ) AS author_name
                FROM paper_authors pa
                LEFT JOIN students s ON s.id = pa.student_id
                LEFT JOIN users us ON us.id = pa.student_id
                LEFT JOIN lecturers l ON l.id = pa.lecturer_id
                LEFT JOIN users ul ON ul.id = pa.lecturer_id
                WHERE pa.paper_id = rp.id
                ORDER BY pa.is_main_author DESC, pa.author_order ASC
                LIMIT 1
            ) author_info ON TRUE
            WHERE COALESCE(rp.approval_status, 'PENDING') = ?
            ORDER BY rp.created_at DESC
            """;

    private static final String UPDATE_POST_MODERATION_SQL = """
            UPDATE posts
            SET approval_status = ?,
                moderator_id = ?,
                moderation_comment = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String UPDATE_PAPER_MODERATION_SQL = """
            UPDATE research_papers
            SET approval_status = ?,
                moderator_id = ?,
                moderation_comment = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String INSERT_MODERATION_LOG_SQL = """
            INSERT INTO moderation_logs (id, moderator_id, target_type, target_id, action, comment, created_at)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

    private static final String SELECT_USER_ID_BY_EMAIL_SQL = """
            SELECT id FROM users WHERE email = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcAdminModerationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ModerationPostResponse> findPostsByStatus(String status) {
        return jdbcTemplate.query(SELECT_PENDING_POSTS_SQL, (rs, rowNum) -> {
            ModerationPostResponse item = new ModerationPostResponse();
            item.setId(rs.getObject("id", UUID.class));
            item.setTitle(rs.getString("title"));
            item.setSummary(rs.getString("description"));
            item.setApprovalStatus(rs.getString("approval_status"));
            item.setAuthorName(rs.getString("author_name"));
            item.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
            return item;
        }, status);
    }

    @Override
    public List<ModerationPaperResponse> findPapersByStatus(String status) {
        return jdbcTemplate.query(SELECT_PENDING_PAPERS_SQL, (rs, rowNum) -> {
            ModerationPaperResponse item = new ModerationPaperResponse();
            item.setId(rs.getObject("id", UUID.class));
            item.setTitle(rs.getString("title"));
            item.setCategory(rs.getString("category"));
            item.setApprovalStatus(rs.getString("approval_status"));
            item.setAuthorName(rs.getString("author_name"));
            item.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
            return item;
        }, status);
    }

    @Override
    public Optional<UUID> findUserIdByEmail(String email) {
        List<UUID> rows = jdbcTemplate.query(SELECT_USER_ID_BY_EMAIL_SQL,
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                email);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    @Override
    public int updatePostModeration(UUID postId, String approvalStatus, UUID moderatorId, String moderationComment) {
        return jdbcTemplate.update(UPDATE_POST_MODERATION_SQL, approvalStatus, moderatorId, moderationComment, postId);
    }

    @Override
    public int updatePaperModeration(UUID paperId, String approvalStatus, UUID moderatorId, String moderationComment) {
        return jdbcTemplate.update(UPDATE_PAPER_MODERATION_SQL, approvalStatus, moderatorId, moderationComment, paperId);
    }

    @Override
    public void insertModerationLog(UUID moderatorId, String targetType, UUID targetId, String action, String comment) {
        jdbcTemplate.update(INSERT_MODERATION_LOG_SQL,
                UUID.randomUUID(),
                moderatorId,
                targetType,
                targetId,
                action,
                comment);
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime();
    }
}
