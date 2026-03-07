package com.hus.mim_backend.infrastructure.adapter.persistence.content;

import com.hus.mim_backend.application.content.dto.ResearchHeroContentResponse;
import com.hus.mim_backend.application.port.output.ResearchHeroContentRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC adapter for research hero content persistence.
 */
@Component
public class JdbcResearchHeroContentRepository implements ResearchHeroContentRepository {
    private static final String SELECT_HERO_SQL = """
            SELECT page_key, title_prefix, title_highlight, subtitle, image_url, updated_at
            FROM research_hero_settings
            WHERE page_key = ?
            """;

    private static final String UPDATE_HERO_SQL = """
            UPDATE research_hero_settings
            SET title_prefix = ?,
                title_highlight = ?,
                subtitle = ?,
                image_url = ?,
                updated_by = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE page_key = ?
            """;

    private static final String INSERT_HERO_SQL = """
            INSERT INTO research_hero_settings (
                id, page_key, title_prefix, title_highlight, subtitle, image_url, updated_by, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

    private static final String SELECT_USER_ID_BY_EMAIL_SQL = """
            SELECT id FROM users WHERE email = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcResearchHeroContentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<ResearchHeroContentResponse> findByPageKey(String pageKey) {
        List<ResearchHeroContentResponse> rows = jdbcTemplate.query(SELECT_HERO_SQL, (rs, rowNum) -> {
            ResearchHeroContentResponse content = new ResearchHeroContentResponse();
            content.setPageKey(rs.getString("page_key"));
            content.setTitlePrefix(rs.getString("title_prefix"));
            content.setTitleHighlight(rs.getString("title_highlight"));
            content.setSubtitle(rs.getString("subtitle"));
            content.setImageUrl(rs.getString("image_url"));
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                content.setUpdatedAt(updatedAt.toLocalDateTime());
            }
            return content;
        }, pageKey);

        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    @Override
    public void upsertByPageKey(String pageKey,
            String titlePrefix,
            String titleHighlight,
            String subtitle,
            String imageUrl,
            UUID updatedBy) {
        int updated = jdbcTemplate.update(
                UPDATE_HERO_SQL,
                titlePrefix,
                titleHighlight,
                subtitle,
                imageUrl,
                updatedBy,
                pageKey);

        if (updated == 0) {
            jdbcTemplate.update(
                    INSERT_HERO_SQL,
                    UUID.randomUUID(),
                    pageKey,
                    titlePrefix,
                    titleHighlight,
                    subtitle,
                    imageUrl,
                    updatedBy);
        }
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
}
