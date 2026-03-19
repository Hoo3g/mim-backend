package com.hus.mim_backend.infrastructure.adapter.persistence.research;

import com.hus.mim_backend.application.port.output.ResearchPaperPageRepository;
import com.hus.mim_backend.application.research.dto.PaperResponse;
import com.hus.mim_backend.application.shared.PagedResult;
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
                   COALESCE(rp.view_count, 0) AS view_count,
                   COALESCE(rp.download_count, 0) AS download_count,
                   (
                       SELECT COUNT(*)
                       FROM saved_research_papers srp
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
            List<String> normalizedResearchAreas,
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

        if (StringUtils.hasText(normalizedKeyword)) {
            params.put("keyword", "%" + normalizedKeyword + "%");
            whereSql.append("\n  AND (")
                    .append(normalizeSql("CONCAT_WS(' ', rp.title, rp.abstract, rp.research_area, rp.journal_conference)"))
                    .append(" LIKE :keyword")
                    .append("\n    OR EXISTS (")
                    .append("\n        SELECT 1")
                    .append("\n        FROM paper_authors pa")
                    .append("\n        LEFT JOIN students s ON s.id = pa.student_id")
                    .append("\n        LEFT JOIN users us ON us.id = pa.student_id")
                    .append("\n        LEFT JOIN lecturers l ON l.id = pa.lecturer_id")
                    .append("\n        LEFT JOIN users ul ON ul.id = pa.lecturer_id")
                    .append("\n        WHERE pa.paper_id = rp.id")
                    .append("\n          AND ")
                    .append(normalizeSql(AUTHOR_NAME_SQL))
                    .append(" LIKE :keyword")
                    .append("\n    )")
                    .append("\n  )");
        }

        String orderBy = resolveOrderBy(metricSort);
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
        response.setViewCount(toInt(row[8]));
        response.setDownloadCount(toInt(row[9]));
        response.setBookmarkCount(toInt(row[10]));
        response.setApprovalStatus(toStringValue(row[11]));
        response.setModerationComment(toStringValue(row[12]));
        response.setCreatedAt(toLocalDateTime(row[13]));
        response.setUpdatedAt(toLocalDateTime(row[14]));
        return response;
    }

    private String resolveOrderBy(String metricSort) {
        return switch (metricSort) {
            case "views" -> "COALESCE(rp.view_count, 0) DESC, rp.created_at DESC";
            case "downloads" -> "COALESCE(rp.download_count, 0) DESC, rp.created_at DESC";
            case "bookmarks" -> "bookmark_count DESC, rp.created_at DESC";
            default -> "rp.created_at DESC";
        };
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

    private String normalizeSql(String expression) {
        return "regexp_replace(unaccent(lower(COALESCE(" + expression + ", ''))), '\\s+', ' ', 'g')";
    }
}
