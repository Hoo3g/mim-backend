package com.hus.mim_backend.infrastructure.adapter.persistence.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hus.mim_backend.application.port.output.PublicPostRepository;
import com.hus.mim_backend.application.post.dto.PublicPostResponse;
import com.hus.mim_backend.application.post.dto.PublicResearchPaperLinkResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC adapter for public recruitment post queries.
 */
@Component
public class JdbcPublicPostRepository implements PublicPostRepository {
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
                   p.approval_status,
                   p.moderation_comment,
                   p.contact_email,
                   p.contact_phone,
                   p.tags,
                   p.created_at,
                   p.updated_at,
                   COALESCE(
                     NULLIF(c.name, ''),
                     NULLIF(TRIM(COALESCE(s.first_name, '') || ' ' || COALESCE(s.last_name, '')), ''),
                     NULLIF(TRIM(COALESCE(l.first_name, '') || ' ' || COALESCE(l.last_name, '')), ''),
                     SPLIT_PART(COALESCE(u.email, ''), '@', 1),
                     'Unknown'
                   ) AS author_name,
                   COALESCE(c.logo_url, l.avatar_url, u.avatar_url) AS author_avatar_url
            FROM posts p
            LEFT JOIN users u ON u.id = p.author_id
            LEFT JOIN companies c ON c.id = p.author_id
            LEFT JOIN students s ON s.id = p.author_id
            LEFT JOIN lecturers l ON l.id = p.author_id
            """;

    private static final String SELECT_ALL_APPROVED_POSTS_SQL = SELECT_POSTS_BASE_SQL + """
            WHERE p.approval_status = 'APPROVED'
            ORDER BY p.created_at DESC
            """;

    private static final String SELECT_APPROVED_POST_BY_ID_SQL = SELECT_POSTS_BASE_SQL + """
            WHERE p.id = ? AND p.approval_status = 'APPROVED'
            """;

    private static final String SELECT_LINKED_RESEARCH_PAPERS_SQL = """
            SELECT rp.id, rp.title, rp.pdf_url
            FROM post_paper_links ppl
            JOIN research_papers rp ON rp.id = ppl.paper_id
            WHERE ppl.post_id = ? AND COALESCE(rp.approval_status, 'PENDING') = 'APPROVED'
            ORDER BY rp.created_at DESC
            """;

    private static final TypeReference<Map<String, Object>> DISPLAY_INFO_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JdbcPublicPostRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<PublicPostResponse> findAllApprovedPosts() {
        return jdbcTemplate.query(SELECT_ALL_APPROVED_POSTS_SQL, (rs, rowNum) -> {
            PublicPostResponse post = mapPost(rs);
            post.setResearchPaperLinks(fetchLinkedPapers(post.getId()));
            return post;
        });
    }

    @Override
    public Optional<PublicPostResponse> findApprovedPostById(UUID postId) {
        List<PublicPostResponse> rows = jdbcTemplate.query(SELECT_APPROVED_POST_BY_ID_SQL, (rs, rowNum) -> {
            PublicPostResponse post = mapPost(rs);
            post.setResearchPaperLinks(fetchLinkedPapers(post.getId()));
            return post;
        }, postId);

        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    private PublicPostResponse mapPost(java.sql.ResultSet rs) throws SQLException {
        PublicPostResponse response = new PublicPostResponse();
        response.setId(rs.getObject("id", UUID.class));
        response.setAuthorId(rs.getObject("author_id", UUID.class));
        response.setAuthorName(rs.getString("author_name"));
        response.setAuthorAvatarUrl(rs.getString("author_avatar_url"));
        response.setTitle(rs.getString("title"));
        response.setDescription(rs.getString("description"));
        response.setRequirements(rs.getString("requirements"));
        response.setBenefits(rs.getString("benefits"));
        response.setAchievements(rs.getString("achievements"));
        response.setPostType(rs.getString("post_type"));
        response.setJobType(rs.getString("job_type"));
        response.setStudentCvUrl(rs.getString("student_cv_url"));
        response.setDisplayInfo(parseDisplayInfo(rs.getString("display_info")));
        response.setLocation(rs.getString("location"));
        response.setSalaryRange(rs.getString("salary_range"));
        response.setStatus(rs.getString("status"));
        response.setApprovalStatus(rs.getString("approval_status"));
        response.setModerationComment(rs.getString("moderation_comment"));
        response.setContactEmail(rs.getString("contact_email"));
        response.setContactPhone(rs.getString("contact_phone"));
        response.setTags(toStringList(rs.getArray("tags")));
        response.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        response.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return response;
    }

    private List<PublicResearchPaperLinkResponse> fetchLinkedPapers(UUID postId) {
        return jdbcTemplate.query(SELECT_LINKED_RESEARCH_PAPERS_SQL, (rs, rowNum) -> {
            PublicResearchPaperLinkResponse item = new PublicResearchPaperLinkResponse();
            item.setId(rs.getObject("id", UUID.class));
            item.setTitle(rs.getString("title"));
            item.setUrl(rs.getString("pdf_url"));
            return item;
        }, postId);
    }

    private Map<String, Object> parseDisplayInfo(String rawDisplayInfo) {
        if (rawDisplayInfo == null || rawDisplayInfo.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(rawDisplayInfo, DISPLAY_INFO_TYPE);
        } catch (Exception ex) {
            return null;
        }
    }

    private List<String> toStringList(Array sqlArray) {
        if (sqlArray == null) {
            return Collections.emptyList();
        }
        try {
            Object value = sqlArray.getArray();
            if (value instanceof String[] values) {
                return List.of(values);
            }
            return Collections.emptyList();
        } catch (SQLException ex) {
            return Collections.emptyList();
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime();
    }
}
