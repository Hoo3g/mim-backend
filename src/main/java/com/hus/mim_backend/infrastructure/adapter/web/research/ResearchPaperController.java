package com.hus.mim_backend.infrastructure.adapter.web.research;

import com.hus.mim_backend.application.research.dto.PaperResponse;
import com.hus.mim_backend.domain.shared.DomainException;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * CRUD endpoints for research papers persisted in PostgreSQL.
 */
@RestController
@RequestMapping(ApiEndpoints.RESEARCH)
public class ResearchPaperController {

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
                   created_at,
                   updated_at
            FROM research_papers
            """;

    private static final String SELECT_PAPER_BY_ID_SQL = SELECT_PAPERS_BASE_SQL + """
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

    private static final String SELECT_USER_ROLES_SQL = """
            SELECT r.name
            FROM roles r
            JOIN user_roles ur ON ur.role_id = r.id
            WHERE ur.user_id = ?
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
                pdf_url = COALESCE(NULLIF(?, ''), pdf_url),
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String DEFAULT_JOURNAL = "MIM Draft";
    private static final String DEFAULT_RESEARCH_AREA = "Chưa phân loại";
    private static final String DEFAULT_PDF_URL = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf";

    private enum AuthorType {
        STUDENT,
        LECTURER
    }

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

    public ResearchPaperController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaperResponse>>> getAllPapers() {
        List<PaperResponse> papers = jdbcTemplate.query(SELECT_ALL_PAPERS_SQL, PAPER_ROW_MAPPER);
        papers.forEach(this::loadAuthors);
        return ResponseEntity.ok(ApiResponse.success(papers, "Get papers successfully"));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<PaperResponse>>> getMyPapers() {
        UUID currentUserId = resolveCurrentUserId();
        List<PaperResponse> papers = jdbcTemplate.query(SELECT_MY_PAPERS_SQL, PAPER_ROW_MAPPER, currentUserId, currentUserId);
        papers.forEach(this::loadAuthors);
        return ResponseEntity.ok(ApiResponse.success(papers, "Get my papers successfully"));
    }

    @GetMapping("/{paperId}")
    public ResponseEntity<ApiResponse<PaperResponse>> getPaperById(@PathVariable UUID paperId) {
        List<PaperResponse> papers = jdbcTemplate.query(SELECT_PAPER_BY_ID_SQL, PAPER_ROW_MAPPER, paperId);
        if (papers.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Research paper not found", "PAPER_NOT_FOUND"));
        }
        PaperResponse paper = papers.getFirst();
        loadAuthors(paper);
        return ResponseEntity.ok(ApiResponse.success(paper, "Get paper successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaperResponse>> createPaper(@RequestBody UpsertPaperRequest request) {
        validateUpsertRequest(request);

        UUID currentUserId = resolveCurrentUserId();
        AuthorType authorType = resolveAuthorType(currentUserId);
        ensureAuthorProfileExists(currentUserId, authorType);

        UUID paperId = UUID.randomUUID();
        String normalizedTitle = request.getTitle().trim();
        String normalizedAbstract = request.getAbstractText().trim();
        String normalizedPdfUrl = normalizePdfUrl(request.getPdfUrl());

        jdbcTemplate.update(INSERT_PAPER_SQL,
                paperId,
                normalizedTitle,
                normalizedAbstract,
                normalizedPdfUrl,
                Year.now().getValue(),
                DEFAULT_JOURNAL,
                DEFAULT_RESEARCH_AREA,
                authorType == AuthorType.LECTURER ? "LECTURER" : "STUDENT");

        UUID studentId = authorType == AuthorType.STUDENT ? currentUserId : null;
        UUID lecturerId = authorType == AuthorType.LECTURER ? currentUserId : null;
        jdbcTemplate.update(INSERT_PAPER_AUTHOR_SQL,
                UUID.randomUUID(),
                paperId,
                studentId,
                lecturerId);

        return getPaperById(paperId);
    }

    @PutMapping("/{paperId}")
    public ResponseEntity<ApiResponse<PaperResponse>> updatePaper(@PathVariable UUID paperId,
            @RequestBody UpsertPaperRequest request) {
        validateUpsertRequest(request);

        UUID currentUserId = resolveCurrentUserId();
        Boolean isOwner = jdbcTemplate.queryForObject(EXISTS_MY_PAPER_SQL, Boolean.class, paperId, currentUserId, currentUserId);
        if (Boolean.FALSE.equals(isOwner)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You do not have permission to update this paper", "FORBIDDEN"));
        }

        int updated = jdbcTemplate.update(UPDATE_PAPER_SQL,
                request.getTitle().trim(),
                request.getAbstractText().trim(),
                normalizePdfUrl(request.getPdfUrl()),
                paperId);
        if (updated == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Research paper not found", "PAPER_NOT_FOUND"));
        }

        return getPaperById(paperId);
    }

    private void loadAuthors(PaperResponse paper) {
        List<PaperResponse.PaperAuthorResponse> authors = jdbcTemplate.query(
                SELECT_AUTHORS_BY_PAPER_SQL,
                PAPER_AUTHOR_ROW_MAPPER,
                paper.getId());
        paper.setAuthors(authors);
    }

    private UUID resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new DomainException("Authentication required");
        }

        String email = String.valueOf(authentication.getPrincipal());
        try {
            return jdbcTemplate.queryForObject(SELECT_USER_ID_BY_EMAIL_SQL, UUID.class, email);
        } catch (EmptyResultDataAccessException ex) {
            throw new DomainException("Authenticated user is not found");
        }
    }

    private AuthorType resolveAuthorType(UUID userId) {
        List<String> roles = jdbcTemplate.queryForList(SELECT_USER_ROLES_SQL, String.class, userId);
        for (String role : roles) {
            if ("LECTURER".equalsIgnoreCase(role)) {
                return AuthorType.LECTURER;
            }
        }
        return AuthorType.STUDENT;
    }

    private void ensureAuthorProfileExists(UUID userId, AuthorType authorType) {
        if (authorType == AuthorType.LECTURER) {
            jdbcTemplate.update(UPSERT_LECTURER_PROFILE_SQL, userId);
            return;
        }
        jdbcTemplate.update(UPSERT_STUDENT_PROFILE_SQL, userId);
    }

    private void validateUpsertRequest(UpsertPaperRequest request) {
        if (request == null || !StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getAbstractText())) {
            throw new DomainException("Title and abstract are required");
        }
    }

    private String normalizePdfUrl(String pdfUrl) {
        if (pdfUrl == null || pdfUrl.isBlank()) {
            return DEFAULT_PDF_URL;
        }
        return pdfUrl.trim();
    }

    public static class UpsertPaperRequest {
        private String title;
        private String abstractText;
        private String pdfUrl;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @JsonProperty("abstract")
        public String getAbstractText() {
            return abstractText;
        }

        @JsonProperty("abstract")
        public void setAbstractText(String abstractText) {
            this.abstractText = abstractText;
        }

        public String getPdfUrl() {
            return pdfUrl;
        }

        public void setPdfUrl(String pdfUrl) {
            this.pdfUrl = pdfUrl;
        }
    }
}
