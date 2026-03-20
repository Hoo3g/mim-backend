package com.hus.mim_backend.infrastructure.adapter.persistence.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hus.mim_backend.application.port.output.PublicPostRepository;
import com.hus.mim_backend.application.post.dto.PublicPostResponse;
import com.hus.mim_backend.application.post.dto.PublicResearchPaperLinkResponse;
import com.hus.mim_backend.infrastructure.adapter.persistence.JdbcMappingUtils;
import com.hus.mim_backend.infrastructure.adapter.persistence.PersistenceSqlFragments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
                   %s AS author_name,
                   COALESCE(c.logo_url, l.avatar_url, u.avatar_url) AS author_avatar_url
            FROM posts p
            LEFT JOIN users u ON u.id = p.author_id
            LEFT JOIN companies c ON c.id = p.author_id
            LEFT JOIN students s ON s.id = p.author_id
            LEFT JOIN lecturers l ON l.id = p.author_id
            """.formatted(PersistenceSqlFragments.AUTHOR_NAME_SQL);

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

    private static final String SELECT_LINKED_RESEARCH_PAPERS_BY_POST_IDS_SQL = """
            SELECT ppl.post_id, rp.id, rp.title, rp.pdf_url
            FROM post_paper_links ppl
            JOIN research_papers rp ON rp.id = ppl.paper_id
            WHERE ppl.post_id IN (:postIds) AND COALESCE(rp.approval_status, 'PENDING') = 'APPROVED'
            ORDER BY ppl.post_id, rp.created_at DESC
            """;

    private static final TypeReference<Map<String, Object>> DISPLAY_INFO_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JdbcPublicPostRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public List<PublicPostResponse> findAllApprovedPosts() {
        List<PublicPostResponse> posts = jdbcTemplate.query(SELECT_ALL_APPROVED_POSTS_SQL, (rs, rowNum) -> mapPost(rs));
        Map<UUID, List<PublicResearchPaperLinkResponse>> linked = fetchLinkedPapersByPostIds(
                posts.stream().map(PublicPostResponse::getId).toList());
        posts.forEach(post -> post.setResearchPaperLinks(linked.getOrDefault(post.getId(), List.of())));
        return posts;
    }

    @Override
    public List<PublicPostResponse> findApprovedPosts(String normalizedKeyword,
            String normalizedType,
            List<String> specializationCandidates) {
        StringBuilder sql = new StringBuilder(SELECT_POSTS_BASE_SQL)
                .append("\nWHERE COALESCE(p.approval_status, 'PENDING') = 'APPROVED'");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if ("company".equals(normalizedType)) {
            sql.append("\n  AND UPPER(COALESCE(p.post_type, '')) LIKE 'COMPANY_%'");
        } else if ("student".equals(normalizedType)) {
            sql.append("\n  AND UPPER(COALESCE(p.post_type, '')) LIKE 'STUDENT_%'");
        }

        if (specializationCandidates != null && !specializationCandidates.isEmpty()) {
            sql.append("\n  AND EXISTS (")
                    .append("\n      SELECT 1")
                    .append("\n      FROM unnest(COALESCE(p.tags, ARRAY[]::text[])) AS tag")
                    .append("\n      WHERE ");

            List<String> clauses = new ArrayList<>();
            for (int i = 0; i < specializationCandidates.size(); i++) {
                String paramName = "specialization" + i;
                String likeParamName = paramName + "Like";
                clauses.add("(" + PersistenceSqlFragments.normalizeSql("tag") + " = :" + paramName
                        + " OR " + PersistenceSqlFragments.normalizeSql("tag") + " LIKE :" + likeParamName + ")");
                params.addValue(paramName, specializationCandidates.get(i));
                params.addValue(likeParamName, "%" + specializationCandidates.get(i) + "%");
            }
            sql.append(String.join(" OR ", clauses)).append("\n  )");
        }

        if (StringUtils.hasText(normalizedKeyword)) {
            params.addValue("keyword", "%" + normalizedKeyword + "%");
            sql.append("\n  AND ")
                    .append(PersistenceSqlFragments.normalizeSql(
                            "CONCAT_WS(' ', p.title, p.description, " + PersistenceSqlFragments.AUTHOR_NAME_SQL
                                    + ", p.requirements, p.achievements, p.benefits)"))
                    .append(" LIKE :keyword");
        }

        sql.append("\nORDER BY p.created_at DESC");

        List<PublicPostResponse> posts = namedParameterJdbcTemplate.query(
                sql.toString(), params, (rs, rowNum) -> mapPost(rs));
        Map<UUID, List<PublicResearchPaperLinkResponse>> linked = fetchLinkedPapersByPostIds(
                posts.stream().map(PublicPostResponse::getId).toList());
        posts.forEach(post -> post.setResearchPaperLinks(linked.getOrDefault(post.getId(), List.of())));
        return posts;
    }

    @Override
    public Optional<PublicPostResponse> findApprovedPostById(UUID postId) {
        List<PublicPostResponse> rows = jdbcTemplate.query(
                SELECT_APPROVED_POST_BY_ID_SQL,
                (rs, rowNum) -> mapPost(rs),
                postId);

        if (rows.isEmpty()) {
            return Optional.empty();
        }
        PublicPostResponse post = rows.getFirst();
        post.setResearchPaperLinks(fetchLinkedPapers(post.getId()));
        return Optional.of(post);
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
        response.setTags(JdbcMappingUtils.toStringList(rs.getArray("tags")));
        response.setCreatedAt(JdbcMappingUtils.toLocalDateTime(rs.getTimestamp("created_at")));
        response.setUpdatedAt(JdbcMappingUtils.toLocalDateTime(rs.getTimestamp("updated_at")));
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

    private Map<UUID, List<PublicResearchPaperLinkResponse>> fetchLinkedPapersByPostIds(List<UUID> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        List<PostPaperLinkRow> rows = namedParameterJdbcTemplate.query(
                SELECT_LINKED_RESEARCH_PAPERS_BY_POST_IDS_SQL,
                new MapSqlParameterSource("postIds", postIds),
                (rs, rowNum) -> {
                    PublicResearchPaperLinkResponse item = new PublicResearchPaperLinkResponse();
                    item.setId(rs.getObject("id", UUID.class));
                    item.setTitle(rs.getString("title"));
                    item.setUrl(rs.getString("pdf_url"));
                    return new PostPaperLinkRow(rs.getObject("post_id", UUID.class), item);
                });

        Map<UUID, List<PublicResearchPaperLinkResponse>> result = new LinkedHashMap<>();
        rows.forEach(row -> result.computeIfAbsent(row.postId(), ignored -> new ArrayList<>()).add(row.paper()));
        return result;
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

    private record PostPaperLinkRow(UUID postId, PublicResearchPaperLinkResponse paper) {}
}
