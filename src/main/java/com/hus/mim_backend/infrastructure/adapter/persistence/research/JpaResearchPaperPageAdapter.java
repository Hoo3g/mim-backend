package com.hus.mim_backend.infrastructure.adapter.persistence.research;

import com.hus.mim_backend.application.port.output.ResearchPaperPageRepository;
import com.hus.mim_backend.application.research.dto.PaperResponse;
import com.hus.mim_backend.application.shared.PagedResult;
import com.hus.mim_backend.infrastructure.adapter.persistence.PersistenceSqlFragments;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA adapter for paged public research paper queries.
 */
@Component
public class JpaResearchPaperPageAdapter implements ResearchPaperPageRepository {
    private static final String AUTHOR_NAME_SQL = """
            COALESCE(
                NULLIF(TRIM(COALESCE(s.first_name, '') || ' ' || COALESCE(s.last_name, '')), ''),
                NULLIF(TRIM(COALESCE(l.first_name, '') || ' ' || COALESCE(l.last_name, '')), ''),
                SPLIT_PART(COALESCE(us.email, ul.email, ''), '@', 1),
                'Unknown'
            )
            """;

    private static final String PAPERS_SELECT_SQL = """
            SELECT rp.id,
                   rp.title,
                   rp.abstract AS abstract_text,
                   rp.pdf_url,
                   rp.publication_year,
                   rp.journal_conference,
                   COALESCE(rp.research_area, 'Chưa phân loại') AS research_area,
                   rp.category,
                   COALESCE(rp.paper_type, 'SCIENTIFIC_RESEARCH') AS paper_type,
                   COALESCE(rp.view_count, 0) AS view_count,
                   COALESCE(rp.download_count, 0) AS download_count,
                   (
                       SELECT COUNT(*)
                       FROM research_paper_unique_bookmarks srp
                       WHERE srp.paper_id = rp.id
                   ) AS bookmark_count,
                   COALESCE(rp.approval_status, 'PENDING') AS approval_status,
                   rp.moderation_comment,
                   rp.created_at,
                   rp.updated_at
            FROM research_papers rp
            """;

    private static final String AUTHORS_BY_PAPER_IDS_SQL_TEMPLATE = """
            SELECT pa.paper_id,
                   COALESCE(pa.student_id, pa.lecturer_id) AS author_id,
                   %s AS author_name,
                   pa.is_main_author,
                   COALESCE(pa.author_order, 1) AS author_order
            FROM paper_authors pa
            LEFT JOIN students s ON s.id = pa.student_id
            LEFT JOIN users us ON us.id = pa.student_id
            LEFT JOIN lecturers l ON l.id = pa.lecturer_id
            LEFT JOIN users ul ON ul.id = pa.lecturer_id
            WHERE pa.paper_id IN (%s)
            ORDER BY pa.paper_id, pa.is_main_author DESC, pa.author_order ASC
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public PagedResult<PaperResponse> findApprovedPapersPage(String normalizedKeyword,
            String normalizedCategory,
            String normalizedPaperType,
            List<String> normalizedResearchAreas,
            Integer publicationYear,
            String metricSort,
            int page,
            int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int offset = safePage * safeSize;

        StringBuilder whereSql = new StringBuilder("\nWHERE COALESCE(rp.approval_status, 'PENDING') = 'APPROVED'");
        Map<String, Object> params = new LinkedHashMap<>();

        if (StringUtils.hasText(normalizedCategory)) {
            whereSql.append("\n  AND LOWER(COALESCE(rp.category, '')) = :category");
            params.put("category", normalizedCategory);
        }

        if (StringUtils.hasText(normalizedPaperType)) {
            whereSql.append("\n  AND UPPER(COALESCE(rp.paper_type, 'SCIENTIFIC_RESEARCH')) = :paperType");
            params.put("paperType", normalizedPaperType);
        }

        if (normalizedResearchAreas != null && !normalizedResearchAreas.isEmpty()) {
            List<String> areaClauses = new ArrayList<>();
            for (int index = 0; index < normalizedResearchAreas.size(); index++) {
                String paramName = "researchArea" + index;
                areaClauses.add(normalizeSql("rp.research_area") + " = :" + paramName);
                params.put(paramName, normalizedResearchAreas.get(index));
            }
            whereSql.append("\n  AND (")
                    .append(String.join(" OR ", areaClauses))
                    .append(")");
        }

        if (publicationYear != null) {
            whereSql.append("\n  AND rp.publication_year = :publicationYear");
            params.put("publicationYear", publicationYear);
        }

        if (StringUtils.hasText(normalizedKeyword)) {
            whereSql.append("\n  AND ")
                    .append(buildKeywordSearchClause(params, normalizedKeyword));
        }

        String orderBy = resolveOrderBy(metricSort, normalizedKeyword);
        String dataSql = PAPERS_SELECT_SQL + whereSql + "\nORDER BY " + orderBy + "\nLIMIT :limit OFFSET :offset";
        Query dataQuery = entityManager.createNativeQuery(dataSql);
        params.forEach(dataQuery::setParameter);
        dataQuery.setParameter("limit", safeSize);
        dataQuery.setParameter("offset", offset);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        List<PaperResponse> content = new ArrayList<>();
        for (Object[] row : rows) {
            content.add(mapPaper(row));
        }

        Map<UUID, List<PaperResponse.PaperAuthorResponse>> authorsByPaperId = fetchAuthorsByPaperIds(
                content.stream()
                        .map(PaperResponse::getId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList());
        content.forEach((paper) -> paper.setAuthors(authorsByPaperId.getOrDefault(paper.getId(), List.of())));

        String countSql = "SELECT COUNT(*) FROM research_papers rp" + whereSql;
        Query countQuery = entityManager.createNativeQuery(countSql);
        params.forEach(countQuery::setParameter);
        long totalElements = toLong(countQuery.getSingleResult());

        return PagedResult.of(content, safePage, safeSize, totalElements);
    }

    private Map<UUID, List<PaperResponse.PaperAuthorResponse>> fetchAuthorsByPaperIds(List<UUID> paperIds) {
        if (paperIds == null || paperIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> params = new LinkedHashMap<>();
        List<String> placeholders = new ArrayList<>();
        for (int index = 0; index < paperIds.size(); index++) {
            String paramName = "paperId" + index;
            placeholders.add(":" + paramName);
            params.put(paramName, paperIds.get(index));
        }

        String sql = AUTHORS_BY_PAPER_IDS_SQL_TEMPLATE.formatted(AUTHOR_NAME_SQL, String.join(", ", placeholders));
        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        Map<UUID, List<PaperResponse.PaperAuthorResponse>> authorsByPaperId = new LinkedHashMap<>();
        for (Object[] row : rows) {
            UUID paperId = toUuid(row[0]);
            PaperResponse.PaperAuthorResponse author = new PaperResponse.PaperAuthorResponse();
            author.setStudentId(toStringValue(row[1]));
            author.setName(toStringValue(row[2]));
            author.setMainAuthor(toBoolean(row[3]));
            author.setAuthorOrder(toInt(row[4]));
            authorsByPaperId.computeIfAbsent(paperId, ignored -> new ArrayList<>()).add(author);
        }
        return authorsByPaperId;
    }

    private PaperResponse mapPaper(Object[] row) {
        PaperResponse response = new PaperResponse();
        response.setId(toUuid(row[0]));
        response.setTitle(toStringValue(row[1]));
        response.setAbstract(toStringValue(row[2]));
        response.setPdfUrl(toStringValue(row[3]));
        response.setPublicationYear(toNullableInt(row[4]));
        response.setJournalConference(toStringValue(row[5]));
        response.setResearchArea(toStringValue(row[6]));
        response.setCategory(toStringValue(row[7]));
        response.setPaperType(toStringValue(row[8]));
        response.setViewCount(toInt(row[9]));
        response.setDownloadCount(toInt(row[10]));
        response.setBookmarkCount(toInt(row[11]));
        response.setApprovalStatus(toStringValue(row[12]));
        response.setModerationComment(toStringValue(row[13]));
        response.setCreatedAt(toLocalDateTime(row[14]));
        response.setUpdatedAt(toLocalDateTime(row[15]));
        return response;
    }

    private String resolveOrderBy(String metricSort, String normalizedKeyword) {
        String fallbackOrderBy = switch (metricSort) {
            case "views" -> "COALESCE(rp.view_count, 0) DESC, rp.created_at DESC";
            case "downloads" -> "COALESCE(rp.download_count, 0) DESC, rp.created_at DESC";
            case "bookmarks" -> "bookmark_count DESC, rp.created_at DESC";
            default -> "rp.created_at DESC";
        };

        if (!StringUtils.hasText(normalizedKeyword)) {
            return fallbackOrderBy;
        }

        return buildKeywordRelevanceOrderBy() + ", " + fallbackOrderBy;
    }

    private String buildKeywordRelevanceOrderBy() {
        String normalizedTitleSql = normalizeSql("rp.title");
        return """
                CASE
                    WHEN %1$s = :keywordExact THEN 0
                    WHEN %1$s LIKE (:keywordExact || '%%') THEN 1
                    WHEN %1$s LIKE :keywordPhrase THEN 2
                    WHEN %1$s %% :keywordExact THEN 3
                    ELSE 4
                END ASC,
                CASE
                    WHEN %1$s LIKE :keywordPhrase THEN 1
                    ELSE 0
                END DESC,
                similarity(%1$s, :keywordExact) DESC,
                word_similarity(%1$s, :keywordExact) DESC,
                word_similarity(:keywordExact, %1$s) DESC,
                ABS(char_length(%1$s) - char_length(:keywordExact)) ASC
                """.formatted(normalizedTitleSql);
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

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private Integer toNullableInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value instanceof String stringValue) {
            return "true".equalsIgnoreCase(stringValue) || "1".equals(stringValue);
        }
        return false;
    }

    private String buildKeywordSearchClause(Map<String, Object> params, String normalizedKeyword) {
        String normalizedContentSql = normalizeSql("rp.title");
        String phraseParam = "keywordPhrase";
        String exactParam = "keywordExact";
        params.put(phraseParam, "%" + normalizedKeyword + "%");
        params.put(exactParam, normalizedKeyword);

        String trigramClause = "("
                + normalizedContentSql + " % :" + exactParam
                + "\n    OR similarity(" + normalizedContentSql + ", :" + exactParam + ") >= 0.22"
                + "\n    OR word_similarity(" + normalizedContentSql + ", :" + exactParam + ") >= 0.45"
                + "\n    OR word_similarity(:" + exactParam + ", " + normalizedContentSql + ") >= 0.45"
                + "\n  )";

        List<String> keywordTokens = PersistenceSqlFragments.splitNormalizedSearchTokens(normalizedKeyword);
        if (keywordTokens.size() == 1 && keywordTokens.get(0).length() <= 2) {
            String wordParam = "keywordWord";
            params.put(wordParam, "% " + keywordTokens.get(0) + " %");
            return "("
                    + buildWholeWordLikeClause(normalizedContentSql, wordParam)
                    + "\n    OR " + buildAuthorExistsWordLikeClause(wordParam)
                    + "\n  )";
        }

        String phraseClause = "("
                + normalizedContentSql + " LIKE :" + phraseParam
                + "\n    OR " + buildAuthorExistsLikeClause(phraseParam)
                + "\n  )";

        if (keywordTokens.size() <= 1) {
            return "(" + phraseClause + "\n    OR " + trigramClause + "\n  )";
        }

        List<String> scoreParts = new ArrayList<>();
        for (int index = 0; index < keywordTokens.size(); index++) {
            String tokenParam = "keywordToken" + index;
            params.put(tokenParam, "%" + keywordTokens.get(index) + "%");

            String tokenClause = "("
                    + normalizedContentSql + " LIKE :" + tokenParam
                    + "\n      OR " + buildAuthorExistsLikeClause(tokenParam)
                    + "\n    )";
            scoreParts.add("CASE WHEN " + tokenClause + " THEN 1 ELSE 0 END");
        }

        int minMatchedTokens = PersistenceSqlFragments.relaxedTokenMatchThreshold(keywordTokens);
        return "(" + phraseClause
                + "\n    OR " + trigramClause
                + "\n    OR ((" + String.join(" + ", scoreParts) + ") >= " + minMatchedTokens + ")"
                + "\n  )";
    }

    private String buildAuthorExistsLikeClause(String parameterName) {
        return """
                EXISTS (
                    SELECT 1
                    FROM paper_authors pa
                    LEFT JOIN students s ON s.id = pa.student_id
                    LEFT JOIN users us ON us.id = pa.student_id
                    LEFT JOIN lecturers l ON l.id = pa.lecturer_id
                    LEFT JOIN users ul ON ul.id = pa.lecturer_id
                    WHERE pa.paper_id = rp.id
                      AND %s LIKE :%s
                )
                """.formatted(
                normalizeSql("COALESCE(NULLIF(pa.author_name_override, ''), " + AUTHOR_NAME_SQL + ")"),
                parameterName
        );
    }

    private String buildAuthorExistsWordLikeClause(String parameterName) {
        return """
                EXISTS (
                    SELECT 1
                    FROM paper_authors pa
                    LEFT JOIN students s ON s.id = pa.student_id
                    LEFT JOIN users us ON us.id = pa.student_id
                    LEFT JOIN lecturers l ON l.id = pa.lecturer_id
                    LEFT JOIN users ul ON ul.id = pa.lecturer_id
                    WHERE pa.paper_id = rp.id
                      AND %s
                )
                """.formatted(
                buildWholeWordLikeClause(
                        normalizeSql("COALESCE(NULLIF(pa.author_name_override, ''), " + AUTHOR_NAME_SQL + ")"),
                        parameterName
                )
        );
    }

    private String buildWholeWordLikeClause(String normalizedSql, String parameterName) {
        return "(' ' || regexp_replace(" + normalizedSql + ", '[^[:alnum:]]+', ' ', 'g') || ' ') LIKE :" + parameterName;
    }

    private String normalizeSql(String expression) {
        return "trim(regexp_replace(immutable_unaccent(lower(COALESCE(" + expression + ", ''))), '[[:space:]]+', ' ', 'g'))";
    }
}
