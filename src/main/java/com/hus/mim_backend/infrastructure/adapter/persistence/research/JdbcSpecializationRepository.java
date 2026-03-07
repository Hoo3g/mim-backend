package com.hus.mim_backend.infrastructure.adapter.persistence.research;

import com.hus.mim_backend.application.port.output.SpecializationRepository;
import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC adapter for specialization taxonomy persistence.
 */
@Component
public class JdbcSpecializationRepository implements SpecializationRepository {
    private static final String SELECT_BASE_SQL = """
            SELECT id,
                   name,
                   sort_order,
                   active,
                   created_at,
                   updated_at
            FROM specializations
            """;

    private static final String SELECT_ACTIVE_SQL = SELECT_BASE_SQL + """
            WHERE active = TRUE
            ORDER BY sort_order ASC, name ASC
            """;

    private static final String SELECT_ALL_SQL = SELECT_BASE_SQL + """
            ORDER BY active DESC, sort_order ASC, name ASC
            """;

    private static final String SELECT_BY_ID_SQL = SELECT_BASE_SQL + """
            WHERE id = ?
            """;

    private static final String SELECT_ACTIVE_NAME_SQL = """
            SELECT name
            FROM specializations
            WHERE active = TRUE
              AND LOWER(name) = LOWER(?)
            LIMIT 1
            """;

    private static final String EXISTS_BY_NAME_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM specializations
                WHERE LOWER(name) = LOWER(?)
            )
            """;

    private static final String EXISTS_OTHER_BY_NAME_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM specializations
                WHERE id <> ?
                  AND LOWER(name) = LOWER(?)
            )
            """;

    private static final String INSERT_SQL = """
            INSERT INTO specializations (
                id, name, sort_order, active, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

    private static final String UPDATE_SQL = """
            UPDATE specializations
            SET name = ?,
                sort_order = ?,
                active = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String DEACTIVATE_SQL = """
            UPDATE specializations
            SET active = FALSE,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final RowMapper<ResearchCategoryResponse> ROW_MAPPER = (rs, rowNum) -> {
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

    public JdbcSpecializationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ResearchCategoryResponse> findActiveSpecializations() {
        return jdbcTemplate.query(SELECT_ACTIVE_SQL, ROW_MAPPER);
    }

    @Override
    public List<ResearchCategoryResponse> findAllSpecializations() {
        return jdbcTemplate.query(SELECT_ALL_SQL, ROW_MAPPER);
    }

    @Override
    public Optional<ResearchCategoryResponse> findById(UUID specializationId) {
        List<ResearchCategoryResponse> rows = jdbcTemplate.query(SELECT_BY_ID_SQL, ROW_MAPPER, specializationId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    @Override
    public Optional<String> findActiveSpecializationName(String specializationName) {
        List<String> rows = jdbcTemplate.query(SELECT_ACTIVE_NAME_SQL,
                (rs, rowNum) -> rs.getString("name"),
                specializationName);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    @Override
    public boolean existsSpecializationWithSameName(String specializationName) {
        Boolean result = jdbcTemplate.queryForObject(EXISTS_BY_NAME_SQL, Boolean.class, specializationName);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public boolean existsOtherSpecializationWithSameName(UUID specializationId, String specializationName) {
        Boolean result = jdbcTemplate.queryForObject(EXISTS_OTHER_BY_NAME_SQL,
                Boolean.class,
                specializationId,
                specializationName);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public UUID createSpecialization(String specializationName, int sortOrder, boolean active) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(INSERT_SQL, id, specializationName, sortOrder, active);
        return id;
    }

    @Override
    public int updateSpecialization(UUID specializationId, String specializationName, int sortOrder, boolean active) {
        return jdbcTemplate.update(UPDATE_SQL, specializationName, sortOrder, active, specializationId);
    }

    @Override
    public int deactivateSpecialization(UUID specializationId) {
        return jdbcTemplate.update(DEACTIVATE_SQL, specializationId);
    }
}
