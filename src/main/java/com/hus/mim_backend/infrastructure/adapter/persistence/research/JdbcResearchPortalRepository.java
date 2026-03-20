package com.hus.mim_backend.infrastructure.adapter.persistence.research;

import com.hus.mim_backend.application.port.output.ResearchPortalRepository;
import com.hus.mim_backend.application.port.output.UserRepository;
import com.hus.mim_backend.application.research.dto.PaperResponse;
import com.hus.mim_backend.infrastructure.adapter.persistence.JdbcMappingUtils;
import com.hus.mim_backend.infrastructure.adapter.persistence.PersistenceSqlFragments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC adapter for research portal persistence operations.
 */
@Component
public class JdbcResearchPortalRepository implements ResearchPortalRepository {
private static final String SELECT_PAPERS_BASE_SQL = """
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

    private static final String SELECT_PAPER_BY_ID_PUBLIC_SQL = SELECT_PAPERS_BASE_SQL + """
            WHERE rp.id = ? AND COALESCE(rp.approval_status, 'PENDING') = 'APPROVED'
            """;

    private static final String SELECT_PAPER_BY_ID_INTERNAL_SQL = SELECT_PAPERS_BASE_SQL + """
            WHERE rp.id = ?
            """;

    private static final String SELECT_MY_PAPERS_SQL = SELECT_PAPERS_BASE_SQL + """
            WHERE rp.id IN (
                SELECT DISTINCT paper_id
                FROM paper_authors
                WHERE student_id = ? OR lecturer_id = ?
            )
            ORDER BY rp.created_at DESC
            """;

    private static final String SELECT_ALL_PAPERS_SQL = SELECT_PAPERS_BASE_SQL + """
            WHERE COALESCE(rp.approval_status, 'PENDING') = 'APPROVED'
            ORDER BY rp.created_at DESC
            """;

    private static final String SELECT_AUTHORS_BY_PAPER_SQL = """
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

    private static final String SELECT_AUTHORS_BY_PAPER_IDS_SQL = """
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
            WHERE pa.paper_id IN (:paperIds)
            ORDER BY pa.paper_id, pa.is_main_author DESC, pa.author_order ASC
            """.formatted(PersistenceSqlFragments.RESEARCH_AUTHOR_NAME_SQL);

    private static final String SELECT_HAS_ROLE_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM roles r
                JOIN user_roles ur ON ur.role_id = r.id
                WHERE ur.user_id = ? AND UPPER(r.name) = UPPER(?)
            )
            """;

    private static final String UPSERT_STUDENT_PROFILE_SQL = """
            INSERT INTO students (id, updated_at)
            VALUES (?, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP
            """;

    private static final String UPSERT_LECTURER_PROFILE_SQL = """
            INSERT INTO lecturers (id, updated_at)
            VALUES (?, CURRENT_TIMESTAMP)
            ON CONFLICT (id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP
            """;

    private static final String SELECT_ACTIVE_CATEGORY_NAME_SQL = """
            SELECT name
            FROM research_categories
            WHERE active = TRUE
              AND LOWER(name) = LOWER(?)
            LIMIT 1
            """;

    private static final String EXISTS_APPROVED_PAPER_BY_PDF_OBJECT_KEY_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM research_papers
                WHERE COALESCE(approval_status, 'PENDING') = 'APPROVED'
                  AND (
                      pdf_url = ?
                      OR pdf_url = '/api/public/storage/research-pdfs/' || ?
                      OR pdf_url = '/api/v1/storage/research-pdfs/' || ?
                      OR pdf_url LIKE '%/api/public/storage/research-pdfs/' || ?
                      OR pdf_url LIKE '%/api/v1/storage/research-pdfs/' || ?
                  )
            )
            """;

    private static final String INSERT_PAPER_SQL = """
            INSERT INTO research_papers (
                id, title, abstract, pdf_url, publication_year,
                journal_conference, research_area, category, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

    private static final String INSERT_PAPER_AUTHOR_SQL = """
            INSERT INTO paper_authors (
                id, paper_id, student_id, lecturer_id, is_main_author, author_order
            )
            VALUES (?, ?, ?, ?, TRUE, 1)
            """;

    private static final String EXISTS_MY_PAPER_SQL = """
            SELECT EXISTS (
                SELECT 1 FROM paper_authors
                WHERE paper_id = ? AND (student_id = ? OR lecturer_id = ?)
            )
            """;

    private static final String UPDATE_PAPER_SQL = """
            UPDATE research_papers
            SET title = ?,
                abstract = ?,
                research_area = ?,
                pdf_url = COALESCE(NULLIF(?, ''), pdf_url),
                approval_status = CASE
                    WHEN COALESCE(approval_status, 'PENDING') = 'REJECTED' THEN 'PENDING'
                    ELSE approval_status
                END,
                moderator_id = CASE
                    WHEN COALESCE(approval_status, 'PENDING') = 'REJECTED' THEN NULL
                    ELSE moderator_id
                END,
                moderation_comment = CASE
                    WHEN COALESCE(approval_status, 'PENDING') = 'REJECTED' THEN NULL
                    ELSE moderation_comment
                END,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String INCREMENT_VIEW_COUNT_SQL = """
            UPDATE research_papers
            SET view_count = COALESCE(view_count, 0) + 1
            WHERE id = ?
              AND COALESCE(approval_status, 'PENDING') = 'APPROVED'
            """;

    private static final String INCREMENT_DOWNLOAD_COUNT_SQL = """
            UPDATE research_papers
            SET download_count = COALESCE(download_count, 0) + 1
            WHERE id = ?
              AND COALESCE(approval_status, 'PENDING') = 'APPROVED'
            """;

    private static final RowMapper<PaperResponse> PAPER_ROW_MAPPER = (rs, rowNum) -> {
        PaperResponse response = new PaperResponse();
        response.setId(rs.getObject("id", UUID.class));
        response.setTitle(rs.getString("title"));
        response.setAbstract(rs.getString("abstract_text"));
        response.setPdfUrl(rs.getString("pdf_url"));
        response.setPublicationYear(rs.getObject("publication_year", Integer.class));
        response.setJournalConference(rs.getString("journal_conference"));
        response.setResearchArea(rs.getString("research_area"));
        response.setCategory(rs.getString("category"));
        response.setViewCount(rs.getInt("view_count"));
        response.setDownloadCount(rs.getInt("download_count"));
        response.setBookmarkCount(rs.getInt("bookmark_count"));
        response.setApprovalStatus(rs.getString("approval_status"));
        response.setModerationComment(rs.getString("moderation_comment"));
        response.setCreatedAt(JdbcMappingUtils.toLocalDateTime(rs.getTimestamp("created_at")));
        response.setUpdatedAt(JdbcMappingUtils.toLocalDateTime(rs.getTimestamp("updated_at")));
        return response;
    };

    private static final RowMapper<PaperResponse.PaperAuthorResponse> PAPER_AUTHOR_ROW_MAPPER = (rs, rowNum) -> {
        PaperResponse.PaperAuthorResponse author = new PaperResponse.PaperAuthorResponse();
        author.setStudentId(rs.getString("author_id"));
        author.setName(rs.getString("author_name"));
        author.setMainAuthor(rs.getBoolean("is_main_author"));
        author.setAuthorOrder(rs.getInt("author_order"));
        return author;
    };

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final UserRepository userRepository;

    public JdbcResearchPortalRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.userRepository = userRepository;
    }

    @Override
    public List<PaperResponse> findAllApprovedPapers() {
        return jdbcTemplate.query(SELECT_ALL_PAPERS_SQL, PAPER_ROW_MAPPER);
    }

    @Override
    public List<PaperResponse> findApprovedPapers(String normalizedKeyword,
            String normalizedCategory,
            List<String> normalizedResearchAreas) {
        StringBuilder sql = new StringBuilder(SELECT_PAPERS_BASE_SQL)
                .append("\nWHERE COALESCE(rp.approval_status, 'PENDING') = 'APPROVED'");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (StringUtils.hasText(normalizedCategory)) {
            sql.append("\n  AND LOWER(COALESCE(rp.category, '')) = :category");
            params.addValue("category", normalizedCategory);
        }

        if (normalizedResearchAreas != null && !normalizedResearchAreas.isEmpty()) {
            sql.append("\n  AND ").append(PersistenceSqlFragments.normalizeSql("rp.research_area")).append(" IN (:researchAreas)");
            params.addValue("researchAreas", normalizedResearchAreas);
        }

        if (StringUtils.hasText(normalizedKeyword)) {
            params.addValue("keyword", "%" + normalizedKeyword + "%");
            sql.append("\n  AND (")
                    .append(PersistenceSqlFragments.normalizeSql("CONCAT_WS(' ', rp.title, rp.abstract, rp.research_area, rp.journal_conference)"))
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
                    .append(PersistenceSqlFragments.normalizeSql(PersistenceSqlFragments.RESEARCH_AUTHOR_NAME_SQL))
                    .append(" LIKE :keyword")
                    .append("\n    )")
                    .append("\n  )");
        }

        sql.append("\nORDER BY rp.created_at DESC");
        return namedParameterJdbcTemplate.query(sql.toString(), params, PAPER_ROW_MAPPER);
    }

    @Override
    public List<PaperResponse> findMyPapers(UUID userId) {
        return jdbcTemplate.query(SELECT_MY_PAPERS_SQL, PAPER_ROW_MAPPER, userId, userId);
    }

    @Override
    public Optional<PaperResponse> findApprovedPaperById(UUID paperId) {
        List<PaperResponse> rows = jdbcTemplate.query(SELECT_PAPER_BY_ID_PUBLIC_SQL, PAPER_ROW_MAPPER, paperId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    @Override
    public Optional<PaperResponse> findPaperById(UUID paperId) {
        List<PaperResponse> rows = jdbcTemplate.query(SELECT_PAPER_BY_ID_INTERNAL_SQL, PAPER_ROW_MAPPER, paperId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    @Override
    @Transactional
    public int incrementApprovedPaperViewCount(UUID paperId) {
        return jdbcTemplate.update(INCREMENT_VIEW_COUNT_SQL, paperId);
    }

    @Override
    @Transactional
    public int incrementApprovedPaperDownloadCount(UUID paperId) {
        return jdbcTemplate.update(INCREMENT_DOWNLOAD_COUNT_SQL, paperId);
    }

    @Override
    public List<PaperResponse.PaperAuthorResponse> findAuthorsByPaperId(UUID paperId) {
        return jdbcTemplate.query(SELECT_AUTHORS_BY_PAPER_SQL, PAPER_AUTHOR_ROW_MAPPER, paperId);
    }

    @Override
    public Map<UUID, List<PaperResponse.PaperAuthorResponse>> findAuthorsByPaperIds(List<UUID> paperIds) {
        if (paperIds == null || paperIds.isEmpty()) {
            return Map.of();
        }

        List<PaperAuthorRow> rows = namedParameterJdbcTemplate.query(
                SELECT_AUTHORS_BY_PAPER_IDS_SQL,
                new MapSqlParameterSource("paperIds", paperIds),
                (rs, rowNum) -> new PaperAuthorRow(
                        rs.getObject("paper_id", UUID.class),
                        PAPER_AUTHOR_ROW_MAPPER.mapRow(rs, rowNum)));

        Map<UUID, List<PaperResponse.PaperAuthorResponse>> authorsByPaperId = new LinkedHashMap<>();
        rows.forEach((row) -> authorsByPaperId
                .computeIfAbsent(row.paperId(), ignored -> new ArrayList<>())
                .add(row.author()));
        return authorsByPaperId;
    }

    @Override
    public Optional<UUID> findUserIdByEmail(String email) {
        return userRepository.findIdByEmail(email);
    }

    @Override
    public boolean hasRole(UUID userId, String roleName) {
        Boolean result = jdbcTemplate.queryForObject(SELECT_HAS_ROLE_SQL, Boolean.class, userId, roleName);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void upsertStudentProfile(UUID userId) {
        jdbcTemplate.update(UPSERT_STUDENT_PROFILE_SQL, userId);
    }

    @Override
    public void upsertLecturerProfile(UUID userId) {
        jdbcTemplate.update(UPSERT_LECTURER_PROFILE_SQL, userId);
    }

    @Override
    public Optional<String> findActiveResearchCategoryName(String researchAreaName) {
        List<String> rows = jdbcTemplate.query(SELECT_ACTIVE_CATEGORY_NAME_SQL,
                (rs, rowNum) -> rs.getString("name"),
                researchAreaName);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    @Override
    public boolean existsApprovedPaperByPdfObjectKey(String objectKey) {
        Boolean result = jdbcTemplate.queryForObject(
                EXISTS_APPROVED_PAPER_BY_PDF_OBJECT_KEY_SQL,
                Boolean.class,
                objectKey,
                objectKey,
                objectKey,
                objectKey,
                objectKey);
        return Boolean.TRUE.equals(result);
    }

    @Override
    @Transactional
    public UUID createPaperWithMainAuthor(UUID userId,
            boolean lecturerAuthor,
            String title,
            String abstractText,
            String pdfUrl,
            int publicationYear,
            String journalConference,
            String researchArea,
            String category) {
        UUID paperId = UUID.randomUUID();
        jdbcTemplate.update(INSERT_PAPER_SQL,
                paperId,
                title,
                abstractText,
                pdfUrl,
                publicationYear,
                journalConference,
                researchArea,
                category);

        UUID studentId = lecturerAuthor ? null : userId;
        UUID lecturerId = lecturerAuthor ? userId : null;
        jdbcTemplate.update(INSERT_PAPER_AUTHOR_SQL,
                UUID.randomUUID(),
                paperId,
                studentId,
                lecturerId);

        return paperId;
    }

    @Override
    public boolean isOwner(UUID paperId, UUID userId) {
        Boolean result = jdbcTemplate.queryForObject(EXISTS_MY_PAPER_SQL, Boolean.class, paperId, userId, userId);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public int updatePaper(UUID paperId, String title, String abstractText, String pdfUrl, String researchArea) {
        return jdbcTemplate.update(UPDATE_PAPER_SQL, title, abstractText, researchArea, pdfUrl, paperId);
    }

    private record PaperAuthorRow(UUID paperId, PaperResponse.PaperAuthorResponse author) {}
}
