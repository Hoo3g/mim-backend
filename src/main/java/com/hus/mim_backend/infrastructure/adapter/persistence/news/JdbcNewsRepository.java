package com.hus.mim_backend.infrastructure.adapter.persistence.news;

import com.hus.mim_backend.application.port.output.NewsRepository;
import com.hus.mim_backend.domain.news.model.News;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC adapter for department news persistence.
 */
@Component
public class JdbcNewsRepository implements NewsRepository {
    private static final String SELECT_BASE_SQL = """
            SELECT id,
                   title,
                   content,
                   summary,
                   image_url,
                   status,
                   pinned,
                   author_id,
                   created_at,
                   updated_at
            FROM news
            """;

    private static final String SELECT_BY_ID_SQL = SELECT_BASE_SQL + """
            WHERE id = ?
            """;

    private static final String SELECT_PUBLISHED_SQL = SELECT_BASE_SQL + """
            WHERE status = 'PUBLISHED'
            ORDER BY pinned DESC, created_at DESC
            """;

    private static final String SELECT_ALL_SQL = SELECT_BASE_SQL + """
            ORDER BY pinned DESC, created_at DESC
            """;

    private static final String EXISTS_BY_ID_SQL = """
            SELECT EXISTS (SELECT 1 FROM news WHERE id = ?)
            """;

    private static final String INSERT_SQL = """
            INSERT INTO news (
                id,
                title,
                content,
                summary,
                image_url,
                status,
                pinned,
                author_id,
                created_at,
                updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

    private static final String UPDATE_SQL = """
            UPDATE news
            SET title = ?,
                content = ?,
                summary = ?,
                image_url = ?,
                status = ?,
                pinned = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String DELETE_SQL = """
            DELETE FROM news
            WHERE id = ?
            """;

    private static final RowMapper<News> NEWS_ROW_MAPPER = (rs, rowNum) -> {
        News news = new News();
        news.setId(rs.getObject("id", UUID.class));
        news.setTitle(rs.getString("title"));
        news.setContent(rs.getString("content"));
        news.setSummary(rs.getString("summary"));
        news.setImageUrl(rs.getString("image_url"));
        news.setStatus(rs.getString("status"));
        news.setPinned(rs.getBoolean("pinned"));
        news.setAuthorId(rs.getObject("author_id", UUID.class));
        news.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        news.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return news;
    };

    private final JdbcTemplate jdbcTemplate;

    public JdbcNewsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<News> findById(UUID id) {
        List<News> rows = jdbcTemplate.query(SELECT_BY_ID_SQL, NEWS_ROW_MAPPER, id);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    @Override
    public List<News> findPublishedOrderByPinnedAndCreatedAtDesc() {
        return jdbcTemplate.query(SELECT_PUBLISHED_SQL, NEWS_ROW_MAPPER);
    }

    @Override
    public List<News> findAllOrderByCreatedAtDesc() {
        return jdbcTemplate.query(SELECT_ALL_SQL, NEWS_ROW_MAPPER);
    }

    @Override
    public News save(News news) {
        if (exists(news.getId())) {
            jdbcTemplate.update(
                    UPDATE_SQL,
                    news.getTitle(),
                    news.getContent(),
                    news.getSummary(),
                    news.getImageUrl(),
                    news.getStatus(),
                    news.isPinned(),
                    news.getId());
        } else {
            jdbcTemplate.update(
                    INSERT_SQL,
                    news.getId(),
                    news.getTitle(),
                    news.getContent(),
                    news.getSummary(),
                    news.getImageUrl(),
                    news.getStatus(),
                    news.isPinned(),
                    news.getAuthorId());
        }

        return findById(news.getId()).orElse(news);
    }

    @Override
    public int deleteById(UUID id) {
        return jdbcTemplate.update(DELETE_SQL, id);
    }

    private boolean exists(UUID id) {
        Boolean result = jdbcTemplate.queryForObject(EXISTS_BY_ID_SQL, Boolean.class, id);
        return Boolean.TRUE.equals(result);
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
