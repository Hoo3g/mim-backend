package com.hus.mim_backend.infrastructure.adapter.persistence;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared SQL fragments reused across multiple JDBC persistence adapters.
 * <p>
 * Centralises repeated SQL expressions to avoid copy-paste bugs.
 * All members are static and this class is not instantiable.
 */
public final class PersistenceSqlFragments {

    /**
     * Resolves the display name of a user who may be a Company, Student, Lecturer,
     * or an anonymous user (fallback to email prefix).
     * Aliases expected in the surrounding query:
     *   u  = users, c = companies, s = students, l = lecturers
     */
    public static final String AUTHOR_NAME_SQL = """
            COALESCE(
              NULLIF(c.name, ''),
              NULLIF(TRIM(COALESCE(s.first_name, '') || ' ' || COALESCE(s.last_name, '')), ''),
              NULLIF(TRIM(COALESCE(l.first_name, '') || ' ' || COALESCE(l.last_name, '')), ''),
              NULLIF(u.full_name, ''),
              SPLIT_PART(COALESCE(u.email, ''), '@', 1),
              'Unknown'
            )
            """;

    /**
     * Resolves the display name of a research-paper author.
     * Aliases expected: s = students, us = users (student), l = lecturers, ul = users (lecturer).
     */
    public static final String RESEARCH_AUTHOR_NAME_SQL = """
            COALESCE(
              NULLIF(TRIM(COALESCE(s.first_name, '') || ' ' || COALESCE(s.last_name, '')), ''),
              NULLIF(TRIM(COALESCE(l.first_name, '') || ' ' || COALESCE(l.last_name, '')), ''),
              NULLIF(COALESCE(us.full_name, ul.full_name), ''),
              SPLIT_PART(COALESCE(us.email, ul.email, ''), '@', 1),
              'Unknown'
            )
            """;

    /**
     * Standard SQL fragment for primary role resolution from user_roles,
     * ordered by role priority (ADMIN &lt; LECTURER &lt; COMPANY &lt; STUDENT).
     * Aliases expected: u = users.
     */
    public static final String PRIMARY_ROLE_SQL = """
            COALESCE((
                SELECT r.name
                FROM roles r
                JOIN user_roles ur ON ur.role_id = r.id
                WHERE ur.user_id = u.id
                ORDER BY CASE r.name
                    WHEN 'ADMIN'    THEN 1
                    WHEN 'LECTURER' THEN 2
                    WHEN 'COMPANY'  THEN 3
                    WHEN 'STUDENT'  THEN 4
                    ELSE 99
                END
                LIMIT 1
            ), 'STUDENT') AS primary_role
            """;

    /**
     * Produces a PostgreSQL expression that normalises a text expression for
     * case-insensitive, accent-insensitive, whitespace-collapsed matching.
     *
     * @param expression any SQL expression producing text
     * @return full SQL expression suitable for a WHERE/LIKE clause
     */
    public static String normalizeSql(String expression) {
        return "trim(regexp_replace(immutable_unaccent(lower(COALESCE(" + expression + ", ''))), '[[:space:]]+', ' ', 'g'))";
    }

    /**
     * Splits an already-normalized keyword into distinct search tokens.
     * Single-character non-numeric tokens are ignored to reduce noisy matches.
     */
    public static List<String> splitNormalizedSearchTokens(String normalizedKeyword) {
        if (normalizedKeyword == null || normalizedKeyword.isBlank()) {
            return List.of();
        }

        Set<String> tokens = new LinkedHashSet<>();
        for (String rawToken : normalizedKeyword.trim().split("[^\\p{L}\\p{N}]+")) {
            String token = rawToken == null ? "" : rawToken.trim();
            if (token.isEmpty()) {
                continue;
            }
            if (token.length() == 1 && !Character.isDigit(token.charAt(0))) {
                continue;
            }
            tokens.add(token);
        }

        if (tokens.isEmpty()) {
            return List.of(normalizedKeyword.trim());
        }
        return List.copyOf(tokens);
    }

    /**
     * Uses a tolerant threshold: for 3+ tokens, one wrong token is still allowed.
     */
    public static int relaxedTokenMatchThreshold(List<String> tokens) {
        int tokenCount = tokens == null ? 0 : tokens.size();
        if (tokenCount <= 1) {
            return tokenCount;
        }
        if (tokenCount == 2) {
            return 2;
        }
        if (tokenCount <= 5) {
            return tokenCount - 1;
        }
        return Math.max(3, (int) Math.ceil(tokenCount * 0.6d));
    }

    /**
     * Convenience constant: default approval status when the DB column is NULL.
     */
    public static final String PENDING_STATUS = "PENDING";

    /**
     * Fallback display name when no other name can be resolved.
     */
    public static final String UNKNOWN_NAME = "Unknown";

    private PersistenceSqlFragments() {
        // utility — no instances
    }
}
