package com.hus.mim_backend.infrastructure.adapter.persistence.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hus.mim_backend.application.port.output.PostPortalRepository;
import com.hus.mim_backend.application.port.output.UserRepository;
import com.hus.mim_backend.application.post.dto.PublicPostResponse;
import com.hus.mim_backend.application.post.dto.PublicResearchPaperLinkResponse;
import com.hus.mim_backend.application.post.dto.UpsertRecruitmentPostRequest;
import com.hus.mim_backend.infrastructure.adapter.persistence.JdbcMappingUtils;
import com.hus.mim_backend.infrastructure.adapter.persistence.PersistenceSqlFragments;
import com.hus.mim_backend.shared.constants.RoleNames;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class JdbcPostPortalRepository implements PostPortalRepository {

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

private static final String SELECT_POSTS_BASE_SQL = """
            SELECT p.id,
                   p.author_id,
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
                   COALESCE(p.approval_status, 'PENDING') AS approval_status,
                   p.moderation_comment,
                   p.contact_email,
                   p.contact_phone,
                   p.tags,
                   p.created_at,
                   p.updated_at,
                   %s AS author_name,
                   COALESCE(c.logo_url, l.avatar_url, u.avatar_url) AS author_avatar_url
            """.formatted(PersistenceSqlFragments.AUTHOR_NAME_SQL)
            + """
            FROM posts p
            LEFT JOIN users u ON u.id = p.author_id
            LEFT JOIN companies c ON c.id = p.author_id
            LEFT JOIN students s ON s.id = p.author_id
            LEFT JOIN lecturers l ON l.id = p.author_id
            """;

    private static final String SELECT_APPROVED_POST_BY_ID_SQL = SELECT_POSTS_BASE_SQL + """
            WHERE p.id = ?
              AND COALESCE(p.approval_status, 'PENDING') = 'APPROVED'
            LIMIT 1
            """;

    private static final String SELECT_POST_BY_ID_FOR_VIEWER_SQL = SELECT_POSTS_BASE_SQL + """
            WHERE p.id = ?
              AND (
                COALESCE(p.approval_status, 'PENDING') = 'APPROVED'
                OR p.author_id = ?
              )
            LIMIT 1
            """;

    private static final String SELECT_POST_BY_ID_FOR_AUTHOR_SQL = SELECT_POSTS_BASE_SQL + """
            WHERE p.id = ? AND p.author_id = ?
            LIMIT 1
            """;

    private static final String SELECT_POSTS_BY_AUTHOR_SQL = SELECT_POSTS_BASE_SQL + """
            WHERE p.author_id = ?
            ORDER BY p.created_at DESC
            """;

    private static final String INSERT_POST_SQL = """
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
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                CAST(? AS jsonb),
                ?,
                ?,
                ?,
                'PENDING',
                ?,
                ?,
                CASE
                    WHEN NULLIF(BTRIM(CAST(? AS text)), '') IS NULL THEN NULL
                    ELSE string_to_array(CAST(? AS text), ',')
                END,
                NULL,
                NULL,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            """;

    private static final String UPDATE_POST_BY_AUTHOR_SQL = """
            UPDATE posts
            SET title          = ?,
                description    = ?,
                requirements   = ?,
                benefits       = ?,
                achievements   = ?,
                post_type      = ?,
                job_type       = ?,
                student_cv_url = ?,
                display_info   = CAST(? AS jsonb),
                location       = ?,
                salary_range   = ?,
                status         = ?,
                contact_email  = ?,
                contact_phone  = ?,
                tags           = CASE
                    WHEN NULLIF(BTRIM(CAST(? AS text)), '') IS NULL THEN NULL
                    ELSE string_to_array(CAST(? AS text), ',')
                END,
                approval_status    = 'PENDING',
                moderator_id       = NULL,
                moderation_comment = NULL,
                updated_at         = CURRENT_TIMESTAMP
            WHERE id = ? AND author_id = ?
            """;

    private static final String DELETE_POST_BY_AUTHOR_SQL = """
            DELETE FROM posts
            WHERE id = ? AND author_id = ?
            """;

    private static final String DELETE_LINKED_RESEARCH_SQL = """
            DELETE FROM post_paper_links WHERE post_id = ?
            """;

    private static final String INSERT_LINKED_RESEARCH_SQL = """
            INSERT INTO post_paper_links (post_id, paper_id)
            SELECT ?, rp.id
            FROM research_papers rp
            WHERE rp.id = ?
            ON CONFLICT DO NOTHING
            """;

    private static final String SELECT_LINKED_RESEARCH_SQL = """
            SELECT rp.id, rp.title, rp.pdf_url
            FROM post_paper_links ppl
            JOIN research_papers rp ON rp.id = ppl.paper_id
            WHERE ppl.post_id = ?
            ORDER BY rp.created_at DESC
            """;

    private static final TypeReference<Map<String, Object>> DISPLAY_INFO_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JdbcPostPortalRepository(JdbcTemplate jdbcTemplate, UserRepository userRepository) {
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
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(rows.getFirst());
    }

    @Override
    public Optional<PublicPostResponse> findPostByIdForViewer(UUID postId, UUID viewerId) {
        List<PublicPostResponse> rows;
        if (viewerId == null) {
            rows = jdbcTemplate.query(
                    SELECT_APPROVED_POST_BY_ID_SQL,
                    (rs, rowNum) -> mapPost(rs),
                    postId);
        } else {
            rows = jdbcTemplate.query(
                    SELECT_POST_BY_ID_FOR_VIEWER_SQL,
                    (rs, rowNum) -> mapPost(rs),
                    postId,
                    viewerId);
        }

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        PublicPostResponse item = rows.getFirst();
        item.setResearchPaperLinks(fetchLinkedResearch(item.getId()));
        return Optional.of(item);
    }

    @Override
    public Optional<PublicPostResponse> findPostByIdForAuthor(UUID postId, UUID authorId) {
        List<PublicPostResponse> rows = jdbcTemplate.query(
                SELECT_POST_BY_ID_FOR_AUTHOR_SQL,
                (rs, rowNum) -> mapPost(rs),
                postId,
                authorId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }

        PublicPostResponse item = rows.getFirst();
        item.setResearchPaperLinks(fetchLinkedResearch(item.getId()));
        return Optional.of(item);
    }

    @Override
    public List<PublicPostResponse> findPostsByAuthor(UUID authorId) {
        return jdbcTemplate.query(
                SELECT_POSTS_BY_AUTHOR_SQL,
                (rs, rowNum) -> {
                    PublicPostResponse item = mapPost(rs);
                    item.setResearchPaperLinks(fetchLinkedResearch(item.getId()));
                    return item;
                },
                authorId);
    }

    @Transactional
    @Override
    public UUID createPost(UUID authorId, UpsertRecruitmentPostRequest request, String displayInfoJson, String tagsCsv) {
        UUID postId = UUID.randomUUID();
        jdbcTemplate.update(INSERT_POST_SQL,
                postId,
                authorId,
                request.getTitle(),
                request.getDescription(),
                request.getRequirements(),
                request.getBenefits(),
                request.getAchievements(),
                request.getPostType(),
                request.getJobType(),
                request.getStudentCvUrl(),
                displayInfoJson,
                request.getLocation(),
                request.getSalaryRange(),
                request.getStatus(),
                request.getContactEmail(),
                request.getContactPhone(),
                tagsCsv,
                tagsCsv);
        return postId;
    }

    @Transactional
    @Override
    public boolean updatePostByAuthor(
            UUID postId,
            UUID authorId,
            UpsertRecruitmentPostRequest request,
            String displayInfoJson,
            String tagsCsv) {
        int affected = jdbcTemplate.update(
                UPDATE_POST_BY_AUTHOR_SQL,
                request.getTitle(),
                request.getDescription(),
                request.getRequirements(),
                request.getBenefits(),
                request.getAchievements(),
                request.getPostType(),
                request.getJobType(),
                request.getStudentCvUrl(),
                displayInfoJson,
                request.getLocation(),
                request.getSalaryRange(),
                request.getStatus(),
                request.getContactEmail(),
                request.getContactPhone(),
                tagsCsv,
                tagsCsv,
                postId,
                authorId);
        return affected > 0;
    }

    @Transactional
    @Override
    public boolean deletePostByAuthor(UUID postId, UUID authorId) {
        return jdbcTemplate.update(DELETE_POST_BY_AUTHOR_SQL, postId, authorId) > 0;
    }

    @Transactional
    @Override
    public void replaceLinkedResearchPapers(UUID postId, List<UUID> paperIds) {
        jdbcTemplate.update(DELETE_LINKED_RESEARCH_SQL, postId);
        if (paperIds == null || paperIds.isEmpty()) {
            return;
        }
        for (UUID paperId : paperIds) {
            jdbcTemplate.update(INSERT_LINKED_RESEARCH_SQL, postId, paperId);
        }
    }

    private PublicPostResponse mapPost(java.sql.ResultSet rs) throws SQLException {
        PublicPostResponse item = new PublicPostResponse();
        item.setId(rs.getObject("id", UUID.class));
        item.setAuthorId(rs.getObject("author_id", UUID.class));
        item.setAuthorName(rs.getString("author_name"));
        item.setAuthorAvatarUrl(rs.getString("author_avatar_url"));
        item.setTitle(rs.getString("title"));
        item.setDescription(rs.getString("description"));
        item.setRequirements(rs.getString("requirements"));
        item.setBenefits(rs.getString("benefits"));
        item.setAchievements(rs.getString("achievements"));
        item.setPostType(rs.getString("post_type"));
        item.setJobType(rs.getString("job_type"));
        item.setStudentCvUrl(rs.getString("student_cv_url"));
        item.setDisplayInfo(parseDisplayInfo(rs.getString("display_info")));
        item.setLocation(rs.getString("location"));
        item.setSalaryRange(rs.getString("salary_range"));
        item.setStatus(rs.getString("status"));
        item.setApprovalStatus(rs.getString("approval_status"));
        item.setModerationComment(rs.getString("moderation_comment"));
        item.setContactEmail(rs.getString("contact_email"));
        item.setContactPhone(rs.getString("contact_phone"));
        item.setTags(JdbcMappingUtils.toStringList(rs.getArray("tags")));
        item.setCreatedAt(JdbcMappingUtils.toLocalDateTime(rs.getTimestamp("created_at")));
        item.setUpdatedAt(JdbcMappingUtils.toLocalDateTime(rs.getTimestamp("updated_at")));
        return item;
    }

    private List<PublicResearchPaperLinkResponse> fetchLinkedResearch(UUID postId) {
        return jdbcTemplate.query(SELECT_LINKED_RESEARCH_SQL, (rs, rowNum) -> {
            PublicResearchPaperLinkResponse link = new PublicResearchPaperLinkResponse();
            link.setId(rs.getObject("id", UUID.class));
            link.setTitle(rs.getString("title"));
            link.setUrl(rs.getString("pdf_url"));
            return link;
        }, postId);
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
}
