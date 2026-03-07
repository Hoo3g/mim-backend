package com.hus.mim_backend.infrastructure.adapter.persistence.research;

import com.hus.mim_backend.application.port.output.ResearchPortalRepository;
import com.hus.mim_backend.application.research.dto.PaperResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC adapter for research portal persistence operations.
 */
@Component
public class JdbcResearchPortalRepository implements ResearchPortalRepository {
    private static final String SELECT_PAPERS_BASE_SQL = """
            SELECT id,
                   title,
                   abstract AS abstract_text,
                   pdf_url,
                   publication_year,
                   journal_conference,
                   COALESCE(research_area, 'Chưa phân loại') AS research_area,
                   category,
                   COALESCE(view_count, 0) AS view_count,
                   COALESCE(approval_status, 'PENDING') AS approval_status,
                   moderation_comment,
                   created_at,
                   updated_at
            FROM research_papers
            """;

    private static final String SELECT_PAPER_BY_ID_PUBLIC_SQL = SELECT_PAPERS_BASE_SQL + """
            WHERE id = ? AND COALESCE(approval_status, 'PENDING') = 'APPROVED'
            """;

    private static final String SELECT_PAPER_BY_ID_INTERNAL_SQL = SELECT_PAPERS_BASE_SQL + """
            WHERE id = ?
            """;

    private static final String SELECT_MY_PAPERS_SQL = SELECT_PAPERS_BASE_SQL + """
            WHERE id IN (
                SELECT DISTINCT paper_id
                FROM paper_authors
                WHERE student_id = ? OR lecturer_id = ?
            )
            ORDER BY created_at DESC
            """;

    private static final String SELECT_ALL_PAPERS_SQL = SELECT_PAPERS_BASE_SQL + """
            WHERE COALESCE(approval_status, 'PENDING') = 'APPROVED'
            ORDER BY created_at DESC
            """;

    private static final String SELECT_AUTHORS_BY_PAPER_SQL = """
            SELECT COALESCE(pa.student_id, pa.lecturer_id) AS author_id,
                   COALESCE(
                       NULLIF(TRIM(COALESCE(s.first_name, '') || ' ' || COALESCE(s.last_name, '')), ''),
                       NULLIF(TRIM(COALESCE(l.first_name, '') || ' ' || COALESCE(l.last_name, '')), ''),
                       SPLIT_PART(COALESCE(us.email, ul.email, ''), '@', 1),
                       'Unknown'
                   ) AS author_name,
                   pa.is_main_author,
                   COALESCE(pa.author_order, 1) AS author_order
            FROM paper_authors pa
            LEFT JOIN students s ON s.id = pa.student_id
            LEFT JOIN users us ON us.id = pa.student_id
            LEFT JOIN lecturers l ON l.id = pa.lecturer_id
            LEFT JOIN users ul ON ul.id = pa.lecturer_id
            WHERE pa.paper_id = ?
            ORDER BY pa.is_main_author DESC, pa.author_order ASC
            """;

    private static final String SELECT_USER_ID_BY_EMAIL_SQL = """
            SELECT id FROM users WHERE email = ?
            """;

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
        response.setApprovalStatus(rs.getString("approval_status"));
        response.setModerationComment(rs.getString("moderation_comment"));
        response.setCreatedAt(rs.getTimestamp("created_at").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        response.setUpdatedAt(rs.getTimestamp("updated_at").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
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

    public JdbcResearchPortalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<PaperResponse> findAllApprovedPapers() {
        return jdbcTemplate.query(SELECT_ALL_PAPERS_SQL, PAPER_ROW_MAPPER);
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
    public List<PaperResponse.PaperAuthorResponse> findAuthorsByPaperId(UUID paperId) {
        return jdbcTemplate.query(SELECT_AUTHORS_BY_PAPER_SQL, PAPER_AUTHOR_ROW_MAPPER, paperId);
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
}
