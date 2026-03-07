package com.hus.mim_backend.infrastructure.adapter.persistence.rbac;

import com.hus.mim_backend.application.port.output.RbacRepository;
import com.hus.mim_backend.application.rbac.model.PermissionDefinition;
import com.hus.mim_backend.application.rbac.model.RolePermissionRow;
import com.hus.mim_backend.application.rbac.model.UserPermissionOverride;
import com.hus.mim_backend.application.rbac.model.UserRbacAssignment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JDBC adapter for RBAC persistence operations.
 */
@Component
public class JdbcRbacRepository implements RbacRepository {
    private static final String SELECT_ROLES_BY_EMAIL_SQL = """
            SELECT DISTINCT r.name
            FROM roles r
            JOIN user_roles ur ON ur.role_id = r.id
            JOIN users u ON u.id = ur.user_id
            WHERE UPPER(u.email) = UPPER(?)
            ORDER BY r.name
            """;

    private static final String SELECT_EFFECTIVE_PERMISSIONS_BY_EMAIL_SQL = """
            WITH user_ctx AS (
                SELECT id
                FROM users
                WHERE UPPER(email) = UPPER(?)
            ),
            role_based AS (
                SELECT DISTINCT p.name AS permission_name
                FROM user_ctx uc
                JOIN user_roles ur ON ur.user_id = uc.id
                JOIN role_permissions rp ON rp.role_id = ur.role_id
                JOIN permissions p ON p.id = rp.permission_id
            ),
            granted AS (
                SELECT DISTINCT p.name AS permission_name
                FROM user_ctx uc
                JOIN user_permissions up ON up.user_id = uc.id AND up.effect = 'GRANT'
                JOIN permissions p ON p.id = up.permission_id
            ),
            denied AS (
                SELECT DISTINCT p.name AS permission_name
                FROM user_ctx uc
                JOIN user_permissions up ON up.user_id = uc.id AND up.effect = 'DENY'
                JOIN permissions p ON p.id = up.permission_id
            )
            SELECT permission_name
            FROM (
                SELECT permission_name FROM role_based
                UNION
                SELECT permission_name FROM granted
            ) allowed
            WHERE permission_name NOT IN (SELECT permission_name FROM denied)
            ORDER BY permission_name
            """;

    private static final String SELECT_EFFECTIVE_PERMISSIONS_BY_USER_ID_SQL = """
            WITH user_ctx AS (
                SELECT ?::uuid AS id
            ),
            role_based AS (
                SELECT DISTINCT p.name AS permission_name
                FROM user_ctx uc
                JOIN user_roles ur ON ur.user_id = uc.id
                JOIN role_permissions rp ON rp.role_id = ur.role_id
                JOIN permissions p ON p.id = rp.permission_id
            ),
            granted AS (
                SELECT DISTINCT p.name AS permission_name
                FROM user_ctx uc
                JOIN user_permissions up ON up.user_id = uc.id AND up.effect = 'GRANT'
                JOIN permissions p ON p.id = up.permission_id
            ),
            denied AS (
                SELECT DISTINCT p.name AS permission_name
                FROM user_ctx uc
                JOIN user_permissions up ON up.user_id = uc.id AND up.effect = 'DENY'
                JOIN permissions p ON p.id = up.permission_id
            )
            SELECT permission_name
            FROM (
                SELECT permission_name FROM role_based
                UNION
                SELECT permission_name FROM granted
            ) allowed
            WHERE permission_name NOT IN (SELECT permission_name FROM denied)
            ORDER BY permission_name
            """;

    private static final String SELECT_PERMISSION_CATALOG_SQL = """
            SELECT p.name,
                   p.description,
                   COALESCE(res.name, 'UNKNOWN') AS resource_name,
                   COALESCE(act.name, 'UNKNOWN') AS action_name
            FROM permissions p
            LEFT JOIN permission_scopes ps ON ps.permission_id = p.id
            LEFT JOIN resources res ON res.id = ps.resource_id
            LEFT JOIN actions act ON act.id = ps.action_id
            ORDER BY p.name
            """;

    private static final String SELECT_ROLE_MATRIX_SQL = """
            SELECT r.name AS role_name,
                   r.description AS role_description,
                   p.name AS permission_name,
                   p.description AS permission_description,
                   COALESCE(res.name, 'UNKNOWN') AS permission_resource,
                   COALESCE(act.name, 'UNKNOWN') AS permission_action
            FROM roles r
            LEFT JOIN role_permissions rp ON rp.role_id = r.id
            LEFT JOIN permissions p ON p.id = rp.permission_id
            LEFT JOIN permission_scopes ps ON ps.permission_id = p.id
            LEFT JOIN resources res ON res.id = ps.resource_id
            LEFT JOIN actions act ON act.id = ps.action_id
            ORDER BY r.name, p.name
            """;

    private static final String SELECT_USERS_WITH_ROLES_SQL = """
            SELECT u.id,
                   u.email,
                   COALESCE(u.account_status, 'PENDING') AS account_status,
                   u.created_at,
                   COALESCE(
                       MAX(NULLIF(c.name, '')),
                       MAX(NULLIF(TRIM(COALESCE(s.first_name, '') || ' ' || COALESCE(s.last_name, '')), '')),
                       MAX(NULLIF(TRIM(COALESCE(l.first_name, '') || ' ' || COALESCE(l.last_name, '')), '')),
                       SPLIT_PART(COALESCE(u.email, ''), '@', 1),
                       'Unknown'
                   ) AS display_name,
                   COALESCE(ARRAY_REMOVE(ARRAY_AGG(DISTINCT r.name), NULL), ARRAY[]::VARCHAR[]) AS roles
            FROM users u
            LEFT JOIN user_roles ur ON ur.user_id = u.id
            LEFT JOIN roles r ON r.id = ur.role_id
            LEFT JOIN companies c ON c.id = u.id
            LEFT JOIN students s ON s.id = u.id
            LEFT JOIN lecturers l ON l.id = u.id
            GROUP BY u.id, u.email, u.account_status, u.created_at
            ORDER BY u.created_at DESC
            """;

    private static final String SELECT_USER_WITH_ROLES_BY_ID_SQL = """
            SELECT u.id,
                   u.email,
                   COALESCE(u.account_status, 'PENDING') AS account_status,
                   u.created_at,
                   COALESCE(
                       MAX(NULLIF(c.name, '')),
                       MAX(NULLIF(TRIM(COALESCE(s.first_name, '') || ' ' || COALESCE(s.last_name, '')), '')),
                       MAX(NULLIF(TRIM(COALESCE(l.first_name, '') || ' ' || COALESCE(l.last_name, '')), '')),
                       SPLIT_PART(COALESCE(u.email, ''), '@', 1),
                       'Unknown'
                   ) AS display_name,
                   COALESCE(ARRAY_REMOVE(ARRAY_AGG(DISTINCT r.name), NULL), ARRAY[]::VARCHAR[]) AS roles
            FROM users u
            LEFT JOIN user_roles ur ON ur.user_id = u.id
            LEFT JOIN roles r ON r.id = ur.role_id
            LEFT JOIN companies c ON c.id = u.id
            LEFT JOIN students s ON s.id = u.id
            LEFT JOIN lecturers l ON l.id = u.id
            WHERE u.id = ?
            GROUP BY u.id, u.email, u.account_status, u.created_at
            """;

    private static final String SELECT_USER_OVERRIDES_SQL = """
            SELECT p.name AS permission_name, up.effect
            FROM user_permissions up
            JOIN permissions p ON p.id = up.permission_id
            WHERE up.user_id = ?
            ORDER BY p.name
            """;

    private static final String SELECT_USER_ID_BY_EMAIL_SQL = """
            SELECT id FROM users WHERE UPPER(email) = UPPER(?)
            """;

    private static final String SELECT_USER_EXISTS_SQL = """
            SELECT EXISTS (SELECT 1 FROM users WHERE id = ?)
            """;

    private static final String SELECT_IS_ADMIN_ACCOUNT_SQL = """
            SELECT EXISTS (
                SELECT 1
                FROM user_roles ur
                JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id = ? AND r.name = 'ADMIN'
            )
            """;

    private static final String SELECT_ROLE_EXISTS_SQL = """
            SELECT EXISTS (SELECT 1 FROM roles WHERE UPPER(name) = UPPER(?))
            """;

    private static final String INSERT_USER_ROLE_SQL = """
            INSERT INTO user_roles (user_id, role_id)
            SELECT ?, r.id
            FROM roles r
            WHERE UPPER(r.name) = UPPER(?)
            ON CONFLICT DO NOTHING
            """;

    private static final String DELETE_USER_ROLE_SQL = """
            DELETE FROM user_roles ur
            USING roles r
            WHERE ur.user_id = ?
              AND ur.role_id = r.id
              AND UPPER(r.name) = UPPER(?)
            """;

    private static final String DELETE_USER_PERMISSION_OVERRIDE_SQL = """
            DELETE FROM user_permissions
            WHERE user_id = ? AND permission_id = ?
            """;

    private static final String UPSERT_USER_PERMISSION_OVERRIDE_SQL = """
            INSERT INTO user_permissions (user_id, permission_id, effect, created_at, updated_at)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (user_id, permission_id) DO UPDATE
            SET effect = EXCLUDED.effect,
                updated_at = CURRENT_TIMESTAMP
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcRbacRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Set<String> findRolesByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return Set.of();
        }
        List<String> rows = jdbcTemplate.queryForList(SELECT_ROLES_BY_EMAIL_SQL, String.class, email.trim());
        return toNormalizedSet(rows);
    }

    @Override
    public Set<String> findEffectivePermissionsByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return Set.of();
        }
        List<String> rows = jdbcTemplate.queryForList(
                SELECT_EFFECTIVE_PERMISSIONS_BY_EMAIL_SQL,
                String.class,
                email.trim());
        return toNormalizedSet(rows);
    }

    @Override
    public Set<String> findEffectivePermissionsByUserId(UUID userId) {
        if (userId == null) {
            return Set.of();
        }
        List<String> rows = jdbcTemplate.queryForList(
                SELECT_EFFECTIVE_PERMISSIONS_BY_USER_ID_SQL,
                String.class,
                userId);
        return toNormalizedSet(rows);
    }

    @Override
    public List<PermissionDefinition> findPermissionCatalog() {
        return jdbcTemplate.query(SELECT_PERMISSION_CATALOG_SQL, (rs, rowNum) -> {
            PermissionDefinition item = new PermissionDefinition();
            item.setName(rs.getString("name"));
            item.setDescription(rs.getString("description"));
            item.setResource(rs.getString("resource_name"));
            item.setAction(rs.getString("action_name"));
            return item;
        });
    }

    @Override
    public List<RolePermissionRow> findRolePermissionRows() {
        return jdbcTemplate.query(SELECT_ROLE_MATRIX_SQL, (rs, rowNum) -> {
            RolePermissionRow row = new RolePermissionRow();
            row.setRoleName(rs.getString("role_name"));
            row.setRoleDescription(rs.getString("role_description"));
            row.setPermissionName(rs.getString("permission_name"));
            row.setPermissionDescription(rs.getString("permission_description"));
            row.setPermissionResource(rs.getString("permission_resource"));
            row.setPermissionAction(rs.getString("permission_action"));
            return row;
        });
    }

    @Override
    public List<UserRbacAssignment> findUserAssignments() {
        return jdbcTemplate.query(SELECT_USERS_WITH_ROLES_SQL, (rs, rowNum) -> {
            UserRbacAssignment user = new UserRbacAssignment();
            user.setUserId(rs.getObject("id", UUID.class));
            user.setEmail(rs.getString("email"));
            user.setDisplayName(rs.getString("display_name"));
            user.setAccountStatus(rs.getString("account_status"));
            user.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
            user.setRoles(toStringList(rs.getArray("roles")));
            return user;
        });
    }

    @Override
    public Optional<UserRbacAssignment> findUserAssignmentById(UUID userId) {
        List<UserRbacAssignment> rows = jdbcTemplate.query(SELECT_USER_WITH_ROLES_BY_ID_SQL, (rs, rowNum) -> {
            UserRbacAssignment user = new UserRbacAssignment();
            user.setUserId(rs.getObject("id", UUID.class));
            user.setEmail(rs.getString("email"));
            user.setDisplayName(rs.getString("display_name"));
            user.setAccountStatus(rs.getString("account_status"));
            user.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
            user.setRoles(toStringList(rs.getArray("roles")));
            return user;
        }, userId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    @Override
    public List<UserPermissionOverride> findUserOverrides(UUID userId) {
        return jdbcTemplate.query(SELECT_USER_OVERRIDES_SQL, (rs, rowNum) -> {
            UserPermissionOverride row = new UserPermissionOverride();
            row.setPermission(rs.getString("permission_name"));
            row.setEffect(rs.getString("effect"));
            return row;
        }, userId);
    }

    @Override
    public Optional<UUID> findUserIdByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return Optional.empty();
        }
        List<UUID> rows = jdbcTemplate.query(SELECT_USER_ID_BY_EMAIL_SQL,
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                email.trim());
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.getFirst());
    }

    @Override
    public boolean existsUser(UUID userId) {
        Boolean exists = jdbcTemplate.queryForObject(SELECT_USER_EXISTS_SQL, Boolean.class, userId);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public boolean isAdminUser(UUID userId) {
        Boolean isAdmin = jdbcTemplate.queryForObject(SELECT_IS_ADMIN_ACCOUNT_SQL, Boolean.class, userId);
        return Boolean.TRUE.equals(isAdmin);
    }

    @Override
    public boolean existsRole(String roleName) {
        if (!StringUtils.hasText(roleName)) {
            return false;
        }
        Boolean exists = jdbcTemplate.queryForObject(SELECT_ROLE_EXISTS_SQL, Boolean.class, roleName.trim());
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void grantRole(UUID userId, String roleName) {
        jdbcTemplate.update(INSERT_USER_ROLE_SQL, userId, roleName);
    }

    @Override
    public int revokeRole(UUID userId, String roleName) {
        return jdbcTemplate.update(DELETE_USER_ROLE_SQL, userId, roleName);
    }

    @Override
    public void replaceOverrides(UUID userId, Set<String> grants, Set<String> denies, Set<String> managedPermissionNames) {
        Map<String, UUID> managedPermissionIds = loadPermissionIds(managedPermissionNames);
        if (managedPermissionIds.size() != managedPermissionNames.size()) {
            throw new IllegalStateException("Permission catalog is inconsistent for managed permissions");
        }

        for (String permissionName : managedPermissionNames) {
            UUID permissionId = managedPermissionIds.get(permissionName);
            jdbcTemplate.update(DELETE_USER_PERMISSION_OVERRIDE_SQL, userId, permissionId);
        }

        for (String permissionName : grants) {
            UUID permissionId = managedPermissionIds.get(permissionName);
            jdbcTemplate.update(UPSERT_USER_PERMISSION_OVERRIDE_SQL, userId, permissionId, "GRANT");
        }

        for (String permissionName : denies) {
            UUID permissionId = managedPermissionIds.get(permissionName);
            jdbcTemplate.update(UPSERT_USER_PERMISSION_OVERRIDE_SQL, userId, permissionId, "DENY");
        }
    }

    private Map<String, UUID> loadPermissionIds(Set<String> permissionNames) {
        if (permissionNames == null || permissionNames.isEmpty()) {
            return Map.of();
        }

        List<String> orderedNames = permissionNames.stream()
                .sorted()
                .toList();
        String placeholders = orderedNames.stream()
                .map(name -> "?")
                .collect(Collectors.joining(", "));
        String sql = "SELECT id, name FROM permissions WHERE name IN (" + placeholders + ")";

        return jdbcTemplate.query(sql, rs -> {
            Map<String, UUID> rows = new LinkedHashMap<>();
            while (rs.next()) {
                rows.put(rs.getString("name"), rs.getObject("id", UUID.class));
            }
            return rows;
        }, orderedNames.toArray());
    }

    private Set<String> toNormalizedSet(List<String> rows) {
        if (rows == null || rows.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String row : rows) {
            if (!StringUtils.hasText(row)) {
                continue;
            }
            normalized.add(row.trim().toUpperCase(Locale.ROOT));
        }
        return normalized;
    }

    private List<String> toStringList(Array sqlArray) {
        if (sqlArray == null) {
            return Collections.emptyList();
        }
        try {
            Object value = sqlArray.getArray();
            if (value instanceof String[] values) {
                return Arrays.stream(values)
                        .filter(StringUtils::hasText)
                        .sorted()
                        .toList();
            }
            return Collections.emptyList();
        } catch (SQLException ex) {
            return Collections.emptyList();
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime();
    }
}
