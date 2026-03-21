package com.hus.mim_backend.infrastructure.adapter.persistence.moderation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hus.mim_backend.application.moderation.dto.ModerationPaperAuthorResponse;
import com.hus.mim_backend.application.port.output.AdminModerationRepository;
import com.hus.mim_backend.application.port.output.UserRepository;
import com.hus.mim_backend.application.moderation.dto.ModerationPaperResponse;
import com.hus.mim_backend.application.moderation.dto.ModerationPostResponse;
import com.hus.mim_backend.application.moderation.dto.ModerationResearchPaperLinkResponse;
import com.hus.mim_backend.infrastructure.adapter.persistence.JdbcMappingUtils;
import com.hus.mim_backend.infrastructure.adapter.persistence.PersistenceSqlFragments;
import com.hus.mim_backend.shared.constants.RoleNames;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
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
                   p.requirements,
                   p.benefits,
                   p.achievements,
                   p.post_type,
                   p.job_type,
                   p.student_cv_url,
                   p.display_info::text AS display_info,
                   p.location,
                   p.salary_range,
                   p.status,
                   p.contact_email,
                   p.contact_phone,
                   p.tags,
                   COALESCE(p.approval_status, 'PENDING') AS approval_status,
                   p.created_at,
                   p.updated_at,
                   COALESCE(c.logo_url, l.avatar_url, u.avatar_url) AS author_avatar_url,
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

    private static final String SELECT_POST_LINKED_RESEARCH_SQL = """
            SELECT rp.id,
                   rp.title,
                   rp.pdf_url
            FROM post_paper_links ppl
            JOIN research_papers rp ON rp.id = ppl.paper_id
            WHERE ppl.post_id = ?
            ORDER BY rp.created_at DESC, rp.title ASC
            """;

    private static final String SELECT_PENDING_PAPERS_SQL = """
            SELECT rp.id,
                   rp.title,
                   rp.category,
                   rp.abstract,
                   rp.pdf_url,
                   rp.publication_year,
                   rp.journal_conference,
                   rp.research_area,
                   COALESCE(rp.approval_status, 'PENDING') AS approval_status,
                   rp.created_at,
                   rp.updated_at,
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

    private static final String SELECT_PAPER_AUTHORS_SQL = """
            SELECT COALESCE(pa.student_id, pa.lecturer_id) AS author_id,
                   %s AS author_name,
                   pa.is_main_author,
                   COALESCE(pa.author_order, 1) AS author_order
            FROM paper_authors pa
            LEFT JOIN students s ON s.id = pa.student_id
            LEFT JOIN users us ON us.id = pa.student_id
            LEFT JOIN lecturers l ON l.id = pa.lecturer_id
            LEFT JOIN users ul ON ul.id = pa.lecturer_id
            WHERE pa.paper_id = ?
            ORDER BY pa.is_main_author DESC, pa.author_order ASC
            """.formatted(PersistenceSqlFragments.RESEARCH_AUTHOR_NAME_SQL);

    private static final String UPDATE_POST_MODERATION_SQL = """
            UPDATE posts
            SET approval_status    = ?,
                moderator_id       = ?,
                moderation_comment = ?,
                updated_at         = CURRENT_TIMESTAMP
            WHERE id = ?
              AND COALESCE(approval_status, 'PENDING') = 'PENDING'
            """;

    private static final String UPDATE_PAPER_MODERATION_SQL = """
            UPDATE research_papers
            SET approval_status    = ?,
                moderator_id       = ?,
                moderation_comment = ?,
                updated_at         = CURRENT_TIMESTAMP
            WHERE id = ?
              AND COALESCE(approval_status, 'PENDING') = 'PENDING'
            """;

    private static final String INSERT_MODERATION_LOG_SQL = """
            INSERT INTO moderation_logs (id, moderator_id, target_type, target_id, action, comment, created_at)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

    private static final String SELECT_ADMIN_EMAILS_SQL = """
            SELECT DISTINCT u.email
            FROM users u
            JOIN user_roles ur ON ur.user_id = u.id
            JOIN roles r ON r.id = ur.role_id
            WHERE UPPER(r.name) = '%s'
              AND u.email IS NOT NULL
              AND TRIM(u.email) <> ''
            ORDER BY u.email
            """.formatted(RoleNames.ADMIN);

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> DISPLAY_INFO_TYPE = new TypeReference<>() {
    };

    public JdbcAdminModerationRepository(JdbcTemplate jdbcTemplate, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    @Override
    public List<ModerationPostResponse> findPostsByStatus(String status) {
        return jdbcTemplate.query(SELECT_PENDING_POSTS_SQL, (rs, rowNum) -> {
            ModerationPostResponse item = new ModerationPostResponse();
            item.setId(rs.getObject("id", UUID.class));
            item.setTitle(rs.getString("title"));
            item.setSummary(rs.getString("description"));
            item.setDescription(rs.getString("description"));
            item.setRequirements(rs.getString("requirements"));
            item.setBenefits(rs.getString("benefits"));
            item.setAchievements(rs.getString("achievements"));
            item.setAuthorAvatarUrl(rs.getString("author_avatar_url"));
            item.setPostType(rs.getString("post_type"));
            item.setJobType(rs.getString("job_type"));
            item.setStudentCvUrl(rs.getString("student_cv_url"));
            item.setDisplayInfo(parseDisplayInfo(rs.getString("display_info")));
            item.setLocation(rs.getString("location"));
            item.setSalaryRange(rs.getString("salary_range"));
            item.setStatus(rs.getString("status"));
            item.setContactEmail(rs.getString("contact_email"));
            item.setContactPhone(rs.getString("contact_phone"));
            item.setTags(JdbcMappingUtils.toStringList(rs.getArray("tags")));
            item.setResearchPaperLinks(fetchLinkedResearch(item.getId()));
            item.setApprovalStatus(rs.getString("approval_status"));
            item.setAuthorName(rs.getString("author_name"));
            item.setCreatedAt(JdbcMappingUtils.toLocalDateTime(rs.getTimestamp("created_at")));
            item.setUpdatedAt(JdbcMappingUtils.toLocalDateTime(rs.getTimestamp("updated_at")));
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
            item.setPaperAbstract(rs.getString("abstract"));
            item.setPdfUrl(rs.getString("pdf_url"));
            Integer publicationYear = (Integer) rs.getObject("publication_year");
            item.setPublicationYear(publicationYear);
            item.setJournalConference(rs.getString("journal_conference"));
            item.setResearchArea(rs.getString("research_area"));
            item.setApprovalStatus(rs.getString("approval_status"));
            item.setAuthorName(rs.getString("author_name"));
            item.setAuthors(fetchPaperAuthors(item.getId()));
            item.setCreatedAt(JdbcMappingUtils.toLocalDateTime(rs.getTimestamp("created_at")));
            item.setUpdatedAt(JdbcMappingUtils.toLocalDateTime(rs.getTimestamp("updated_at")));
            return item;
        }, status);
    }

    private List<ModerationResearchPaperLinkResponse> fetchLinkedResearch(UUID postId) {
        return jdbcTemplate.query(SELECT_POST_LINKED_RESEARCH_SQL, (rs, rowNum) -> {
            ModerationResearchPaperLinkResponse item = new ModerationResearchPaperLinkResponse();
            item.setId(rs.getObject("id", UUID.class));
            item.setTitle(rs.getString("title"));
            item.setUrl(rs.getString("pdf_url"));
            return item;
        }, postId);
    }

    private List<ModerationPaperAuthorResponse> fetchPaperAuthors(UUID paperId) {
        return jdbcTemplate.query(SELECT_PAPER_AUTHORS_SQL, (rs, rowNum) -> {
            ModerationPaperAuthorResponse author = new ModerationPaperAuthorResponse();
            author.setAuthorId(rs.getString("author_id"));
            author.setName(rs.getString("author_name"));
            author.setMainAuthor(rs.getBoolean("is_main_author"));
            author.setAuthorOrder(rs.getInt("author_order"));
            return author;
        }, paperId);
    }

    private Map<String, Object> parseDisplayInfo(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, DISPLAY_INFO_TYPE);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Optional<UUID> findUserIdByEmail(String email) {
        return userRepository.findIdByEmail(email);
    }

    @Override
    public List<String> findAdminEmails() {
        return jdbcTemplate.queryForList(SELECT_ADMIN_EMAILS_SQL, String.class);
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
}
