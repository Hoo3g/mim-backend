package com.hus.mim_backend.infrastructure.adapter.persistence.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hus.mim_backend.application.port.output.PublicPostPageRepository;
import com.hus.mim_backend.application.post.dto.PublicPostResponse;
import com.hus.mim_backend.application.post.dto.PublicResearchPaperLinkResponse;
import com.hus.mim_backend.application.shared.PagedResult;
import com.hus.mim_backend.infrastructure.adapter.persistence.PersistenceSqlFragments;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Array;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * JPA adapter for paged public recruitment post queries.
 */
@Component
public class JpaPublicPostPageAdapter implements PublicPostPageRepository {
    private static final String AUTHOR_NAME_SQL = """
            COALESCE(
              NULLIF(c.name, ''),
              NULLIF(TRIM(COALESCE(s.first_name, '') || ' ' || COALESCE(s.last_name, '')), ''),
              NULLIF(TRIM(COALESCE(l.first_name, '') || ' ' || COALESCE(l.last_name, '')), ''),
              SPLIT_PART(COALESCE(u.email, ''), '@', 1),
              'Unknown'
            )
            """;

    private static final String POSTS_FROM_SQL = """
            FROM posts p
            LEFT JOIN users u ON u.id = p.author_id
            LEFT JOIN companies c ON c.id = p.author_id
            LEFT JOIN students s ON s.id = p.author_id
            LEFT JOIN lecturers l ON l.id = p.author_id
            """;

    private static final String POSTS_SELECT_SQL = """
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
            """.formatted(AUTHOR_NAME_SQL);

    private static final String SELECT_LINKED_RESEARCH_PAPERS_SQL = """
            SELECT rp.id, rp.title, rp.pdf_url
            FROM post_paper_links ppl
            JOIN research_papers rp ON rp.id = ppl.paper_id
            WHERE ppl.post_id = :postId
              AND COALESCE(rp.approval_status, 'PENDING') = 'APPROVED'
            ORDER BY rp.created_at DESC
            """;

    private static final TypeReference<Map<String, Object>> DISPLAY_INFO_TYPE = new TypeReference<>() {
    };

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PagedResult<PublicPostResponse> findApprovedPostsPage(String normalizedKeyword,
            String normalizedType,
            List<String> categoryCandidates,
            int page,
            int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int offset = safePage * safeSize;

        StringBuilder whereSql = new StringBuilder("\nWHERE COALESCE(p.approval_status, 'PENDING') = 'APPROVED'");
        Map<String, Object> params = new LinkedHashMap<>();

        if ("company".equals(normalizedType)) {
            whereSql.append("\n  AND UPPER(COALESCE(p.post_type, '')) LIKE 'COMPANY_%'");
        } else if ("student".equals(normalizedType)) {
            whereSql.append("\n  AND UPPER(COALESCE(p.post_type, '')) LIKE 'STUDENT_%'");
        }

        if (categoryCandidates != null && !categoryCandidates.isEmpty()) {
            List<String> clauses = new ArrayList<>();
            String normalizedContentSql = normalizeSql(
                    "CONCAT_WS(' ', p.title, p.description, " + AUTHOR_NAME_SQL
                            + ", p.requirements, p.achievements, p.benefits, p.location)");

            for (int index = 0; index < categoryCandidates.size(); index++) {
                String parameterName = "category" + index;
                String likeParameterName = parameterName + "Like";
                String wordLikeParameterName = parameterName + "WordLike";
                String category = categoryCandidates.get(index);
                String contentClause = category.length() <= 2
                        ? "(' ' || " + normalizedContentSql + " || ' ') LIKE :" + wordLikeParameterName
                        : normalizedContentSql + " LIKE :" + likeParameterName;

                clauses.add("""
                        (
                            EXISTS (
                                SELECT 1
                                FROM unnest(COALESCE(p.tags, ARRAY[]::text[])) AS tag_row(tag_value)
                                WHERE %s = :%s
                            )
                            OR %s
                        )
                        """.formatted(normalizeSql("tag_value"), parameterName, contentClause));
                params.put(parameterName, category);
                if (category.length() <= 2) {
                    params.put(wordLikeParameterName, "% " + category + " %");
                } else {
                    params.put(likeParameterName, "%" + category + "%");
                }
            }

            whereSql.append("\n  AND (")
                    .append(String.join(" OR ", clauses))
                    .append("\n  )");
        }

        if (StringUtils.hasText(normalizedKeyword)) {
            whereSql.append("\n  AND ")
                    .append(buildKeywordSearchClause(params, normalizedKeyword));
        }

        String dataSql = POSTS_SELECT_SQL + POSTS_FROM_SQL + whereSql + "\nORDER BY p.created_at DESC"
                + "\nLIMIT :limit OFFSET :offset";
        Query dataQuery = entityManager.createNativeQuery(dataSql);
        params.forEach(dataQuery::setParameter);
        dataQuery.setParameter("limit", safeSize);
        dataQuery.setParameter("offset", offset);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        List<PublicPostResponse> content = rows.stream()
                .map(this::mapPost)
                .toList();

        content.forEach((post) -> post.setResearchPaperLinks(fetchLinkedPapers(post.getId())));

        String countSql = "SELECT COUNT(*) " + POSTS_FROM_SQL + whereSql;
        Query countQuery = entityManager.createNativeQuery(countSql);
        params.forEach(countQuery::setParameter);
        long totalElements = toLong(countQuery.getSingleResult());

        return PagedResult.of(content, safePage, safeSize, totalElements);
    }

    private PublicPostResponse mapPost(Object[] row) {
        PublicPostResponse response = new PublicPostResponse();
        response.setId(toUuid(row[0]));
        response.setAuthorId(toUuid(row[1]));
        response.setTitle(toStringValue(row[2]));
        response.setDescription(toStringValue(row[3]));
        response.setRequirements(toStringValue(row[4]));
        response.setBenefits(toStringValue(row[5]));
        response.setAchievements(toStringValue(row[6]));
        response.setPostType(toStringValue(row[7]));
        response.setJobType(toStringValue(row[8]));
        response.setStudentCvUrl(toStringValue(row[9]));
        response.setDisplayInfo(parseDisplayInfo(toStringValue(row[10])));
        response.setLocation(toStringValue(row[11]));
        response.setSalaryRange(toStringValue(row[12]));
        response.setStatus(toStringValue(row[13]));
        response.setApprovalStatus(toStringValue(row[14]));
        response.setModerationComment(toStringValue(row[15]));
        response.setContactEmail(toStringValue(row[16]));
        response.setContactPhone(toStringValue(row[17]));
        response.setTags(toTags(row[18]));
        response.setCreatedAt(toLocalDateTime(row[19]));
        response.setUpdatedAt(toLocalDateTime(row[20]));
        response.setAuthorName(toStringValue(row[21]));
        response.setAuthorAvatarUrl(toStringValue(row[22]));
        return response;
    }

    private List<PublicResearchPaperLinkResponse> fetchLinkedPapers(UUID postId) {
        if (postId == null) {
            return List.of();
        }

        Query query = entityManager.createNativeQuery(SELECT_LINKED_RESEARCH_PAPERS_SQL);
        query.setParameter("postId", postId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<PublicResearchPaperLinkResponse> links = new ArrayList<>();
        for (Object[] row : rows) {
            PublicResearchPaperLinkResponse item = new PublicResearchPaperLinkResponse();
            item.setId(toUuid(row[0]));
            item.setTitle(toStringValue(row[1]));
            item.setUrl(toStringValue(row[2]));
            links.add(item);
        }
        return links;
    }

    private Map<String, Object> parseDisplayInfo(String rawDisplayInfo) {
        if (!StringUtils.hasText(rawDisplayInfo)) {
            return null;
        }
        try {
            return objectMapper.readValue(rawDisplayInfo, DISPLAY_INFO_TYPE);
        } catch (Exception ex) {
            return null;
        }
    }

    private List<String> toTags(Object rawValue) {
        if (rawValue == null) {
            return Collections.emptyList();
        }

        try {
            if (rawValue instanceof Array sqlArray) {
                Object value = sqlArray.getArray();
                if (value instanceof String[] values) {
                    return distinctNonBlank(values);
                }
                if (value instanceof Object[] values) {
                    return distinctNonBlank(values);
                }
                return Collections.emptyList();
            }
            if (rawValue instanceof String value) {
                return parsePostgresArrayString(value);
            }
            if (rawValue instanceof Object[] values) {
                return distinctNonBlank(values);
            }
        } catch (Exception ignored) {
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    private List<String> parsePostgresArrayString(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return Collections.emptyList();
        }
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.isBlank()) {
            return Collections.emptyList();
        }
        return distinctNonBlank(trimmed.split(","));
    }

    private List<String> distinctNonBlank(Object[] values) {
        Set<String> result = new LinkedHashSet<>();
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (text.isEmpty()) {
                continue;
            }
            result.add(text);
        }
        return new ArrayList<>(result);
    }

    private UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(String.valueOf(value));
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toLocalDateTime();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return null;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private String buildKeywordSearchClause(Map<String, Object> params, String normalizedKeyword) {
        String normalizedContentSql = normalizeSql(
                "CONCAT_WS(' ', p.title, p.description, " + AUTHOR_NAME_SQL
                        + ", p.requirements, p.achievements, p.benefits)"
        );
        String phraseParam = "keywordPhrase";
        params.put(phraseParam, "%" + normalizedKeyword + "%");

        String phraseClause = normalizedContentSql + " LIKE :" + phraseParam;
        List<String> keywordTokens = PersistenceSqlFragments.splitNormalizedSearchTokens(normalizedKeyword);
        if (keywordTokens.size() <= 1) {
            return phraseClause;
        }

        List<String> scoreParts = new ArrayList<>();
        for (int index = 0; index < keywordTokens.size(); index++) {
            String tokenParam = "keywordToken" + index;
            params.put(tokenParam, "%" + keywordTokens.get(index) + "%");
            scoreParts.add("CASE WHEN " + normalizedContentSql + " LIKE :" + tokenParam + " THEN 1 ELSE 0 END");
        }

        int minMatchedTokens = PersistenceSqlFragments.relaxedTokenMatchThreshold(keywordTokens);
        return "(" + phraseClause + " OR ((" + String.join(" + ", scoreParts) + ") >= " + minMatchedTokens + "))";
    }

    private String normalizeSql(String expression) {
        return "trim(regexp_replace(immutable_unaccent(lower(COALESCE(" + expression + ", ''))), '[[:space:]]+', ' ', 'g'))";
    }
}
