package com.hus.mim_backend.infrastructure.adapter.persistence.research;

import com.hus.mim_backend.application.port.output.ResearchCategoryRepository;
import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC adapter for research category taxonomy persistence.
 */
@Component
public class JdbcResearchCategoryRepository implements ResearchCategoryRepository {
    private static final String SELECT_CATEGORY_BASE_SQL = """
            SELECT id,
                   name,
                   sort_order,
                   active,
                   created_at,
                   updated_at
            FROM research_categories
            """;

    private static final String SELECT_ACTIVE_CATEGORIES_SQL = SELECT_CATEGORY_BASE_SQL + """
            WHERE active = TRUE
            ORDER BY sort_order ASC, name ASC
            """;

    private static final String SELECT_ALL_CATEGORIES_SQL = SELECT_CATEGORY_BASE_SQL + """
            ORDER BY active DESC, sort_order ASC, name ASC
            """;

    private static final String SELECT_CATEGORY_BY_ID_SQL = SELECT_CATEGORY_BASE_SQL + """
            WHERE id = ?
            """;

    private static final String SELECT_ACTIVE_CATEGORY_NAME_SQL = """
            SELECT name
            FROM research_categories
            WHERE active = TRUE AND LOWER(name) = LOWER(?)
            LIMIT 1
            """;

    private static final String SELECT_CATEGORY_NAME_BY_ID_SQL = """
            SELECT name
            FROM research_categories
            WHERE id = ?
            LIMIT 1
            """;

    private static final String EXISTS_CATEGORY_BY_NAME_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM research_categories
                WHERE LOWER(name) = LOWER(?)
            )
            """;

    private static final String EXISTS_OTHER_CATEGORY_BY_NAME_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM research_categories
                WHERE id <> ?
                  AND LOWER(name) = LOWER(?)
            )
            """;

    private static final String INSERT_CATEGORY_SQL = """
            INSERT INTO research_categories (
                id, name, sort_order, active, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

    private static final String UPDATE_CATEGORY_SQL = """
            UPDATE research_categories
            SET name = ?,
                sort_order = ?,
                active = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String DEACTIVATE_CATEGORY_SQL = """
            UPDATE research_categories
            SET active = FALSE,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final RowMapper<ResearchCategoryResponse> CATEGORY_ROW_MAPPER = (rs, rowNum) -> {
        ResearchCategoryResponse response = new ResearchCategoryResponse();
        response.setId(rs.getObject("id", UUID.class));
        response.setName(rs.getString("name"));
        response.setSortOrder(rs.getInt("sort_order"));
        response.setActive(rs.getBoolean("active"));
        response.setCreatedAt(rs.getTimestamp("created_at").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        response.setUpdatedAt(rs.getTimestamp("updated_at").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        return response;
    };

    private final JdbcTemplate jdbcTemplate;

    public JdbcResearchCategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ResearchCategoryResponse> findActiveCategories() {
        return jdbcTemplate.query(SELECT_ACTIVE_CATEGORIES_SQL, CATEGORY_ROW_MAPPER);
    }

    @Override
    public List<ResearchCategoryResponse> findAllCategories() {
        return jdbcTemplate.query(SELECT_ALL_CATEGORIES_SQL, CATEGORY_ROW_MAPPER);
    }

    @Override
    public Optional<ResearchCategoryResponse> findById(UUID categoryId) {
        List<ResearchCategoryResponse> rows = jdbcTemplate.query(SELECT_CATEGORY_BY_ID_SQL, CATEGORY_ROW_MAPPER, categoryId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    @Override
    public Optional<String> findActiveCategoryName(String categoryName) {
        List<String> rows = jdbcTemplate.query(SELECT_ACTIVE_CATEGORY_NAME_SQL,
                (rs, rowNum) -> rs.getString("name"),
                categoryName);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    @Override
    public Optional<String> findCategoryNameById(UUID categoryId) {
        List<String> rows = jdbcTemplate.query(SELECT_CATEGORY_NAME_BY_ID_SQL,
                (rs, rowNum) -> rs.getString("name"),
                categoryId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    @Override
    public boolean existsCategoryWithSameName(String categoryName) {
        Boolean result = jdbcTemplate.queryForObject(EXISTS_CATEGORY_BY_NAME_SQL, Boolean.class, categoryName);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public boolean existsOtherCategoryWithSameName(UUID categoryId, String categoryName) {
        Boolean result = jdbcTemplate.queryForObject(EXISTS_OTHER_CATEGORY_BY_NAME_SQL,
                Boolean.class,
                categoryId,
                categoryName);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public UUID createCategory(String categoryName, int sortOrder, boolean active) {
        UUID categoryId = UUID.randomUUID();
        jdbcTemplate.update(INSERT_CATEGORY_SQL, categoryId, categoryName, sortOrder, active);
        return categoryId;
    }

    @Override
    public int updateCategory(UUID categoryId, String categoryName, int sortOrder, boolean active) {
        return jdbcTemplate.update(UPDATE_CATEGORY_SQL, categoryName, sortOrder, active, categoryId);
    }

    @Override
    public int deactivateCategory(UUID categoryId) {
        return jdbcTemplate.update(DEACTIVATE_CATEGORY_SQL, categoryId);
    }
}
