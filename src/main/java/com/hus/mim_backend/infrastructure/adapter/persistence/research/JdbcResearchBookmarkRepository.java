package com.hus.mim_backend.infrastructure.adapter.persistence.research;

import com.hus.mim_backend.application.port.output.ResearchBookmarkRepository;
import com.hus.mim_backend.application.research.dto.ResearchBookmarkResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JdbcResearchBookmarkRepository implements ResearchBookmarkRepository {
    private static final String SELECT_USER_ID_BY_EMAIL_SQL = """
            SELECT id FROM users WHERE email = ?
            """;

    private static final String EXISTS_APPROVED_PAPER_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM research_papers
                WHERE id = ?
                  AND COALESCE(approval_status, 'PENDING') = 'APPROVED'
            )
            """;

    private static final String INSERT_BOOKMARK_SQL = """
            INSERT INTO saved_research_papers (user_id, paper_id, created_at)
            VALUES (?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (user_id, paper_id) DO NOTHING
            """;

    private static final String DELETE_BOOKMARK_SQL = """
            DELETE FROM saved_research_papers
            WHERE user_id = ? AND paper_id = ?
            """;

    private static final String SELECT_BOOKMARKS_SQL = """
            SELECT sr.paper_id,
                   rp.title,
                   COALESCE(rp.research_area, 'Chưa phân loại') AS research_area,
                   COALESCE(rp.category, 'STUDENT') AS category,
                   rp.publication_year,
                   sr.created_at
            FROM saved_research_papers sr
            JOIN research_papers rp ON rp.id = sr.paper_id
            WHERE sr.user_id = ?
              AND COALESCE(rp.approval_status, 'PENDING') = 'APPROVED'
            ORDER BY sr.created_at DESC
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcResearchBookmarkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UUID> findUserIdByEmail(String email) {
        List<UUID> ids = jdbcTemplate.query(SELECT_USER_ID_BY_EMAIL_SQL,
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                email);
        if (ids.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ids.getFirst());
    }

    @Override
    public boolean existsApprovedPaper(UUID paperId) {
        Boolean result = jdbcTemplate.queryForObject(EXISTS_APPROVED_PAPER_SQL, Boolean.class, paperId);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void saveBookmark(UUID userId, UUID paperId) {
        jdbcTemplate.update(INSERT_BOOKMARK_SQL, userId, paperId);
    }

    @Override
    public void deleteBookmark(UUID userId, UUID paperId) {
        jdbcTemplate.update(DELETE_BOOKMARK_SQL, userId, paperId);
    }

    @Override
    public List<ResearchBookmarkResponse> findBookmarksByUserId(UUID userId) {
        return jdbcTemplate.query(SELECT_BOOKMARKS_SQL, (rs, rowNum) -> {
            ResearchBookmarkResponse item = new ResearchBookmarkResponse();
            item.setPaperId(rs.getObject("paper_id", UUID.class));
            item.setTitle(rs.getString("title"));
            item.setResearchArea(rs.getString("research_area"));
            item.setCategory(rs.getString("category"));
            item.setPublicationYear(rs.getObject("publication_year", Integer.class));
            item.setSavedAt(toLocalDateTime(rs.getTimestamp("created_at")));
            return item;
        }, userId);
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime();
    }
}
