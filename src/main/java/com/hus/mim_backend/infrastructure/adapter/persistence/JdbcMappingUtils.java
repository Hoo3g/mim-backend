package com.hus.mim_backend.infrastructure.adapter.persistence;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Static mapping helpers for JDBC result-set conversion.
 * <p>
 * Eliminates repeated private methods scattered across every JDBC adapter.
 */
public final class JdbcMappingUtils {

    /**
     * Converts a JDBC {@link Timestamp} to {@link LocalDateTime}.
     * Returns {@code null} if the timestamp is {@code null}.
     */
    public static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime();
    }

    /**
     * Converts a raw value returned by {@code EntityManager.createNativeQuery()}
     * to {@link LocalDateTime}.
     * Handles {@link Timestamp}, {@link OffsetDateTime}, and {@link LocalDateTime} inputs.
     * Returns {@code null} for null or unrecognised types.
     */
    public static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (value instanceof OffsetDateTime odt) {
            return odt.toLocalDateTime();
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        return null;
    }

    /**
     * Converts a PostgreSQL {@code text[]} array returned by JDBC to a {@link List}.
     * Returns an empty list for {@code null} or unreadable arrays.
     */
    public static List<String> toStringList(Array sqlArray) {
        if (sqlArray == null) {
            return Collections.emptyList();
        }
        try {
            Object value = sqlArray.getArray();
            if (value instanceof String[] values) {
                return List.of(values);
            }
            if (value instanceof Object[] values) {
                return Arrays.stream(values)
                        .filter(o -> o != null)
                        .map(Object::toString)
                        .toList();
            }
            return Collections.emptyList();
        } catch (SQLException ex) {
            return Collections.emptyList();
        }
    }

    /**
     * Converts a PostgreSQL {@code text[]} array to a sorted, de-duplicated list
     * of non-blank strings (used for RBAC role lists).
     */
    public static List<String> toSortedStringList(Array sqlArray) {
        if (sqlArray == null) {
            return Collections.emptyList();
        }
        try {
            Object value = sqlArray.getArray();
            if (value instanceof String[] values) {
                return Arrays.stream(values)
                        .filter(s -> s != null && !s.isBlank())
                        .sorted()
                        .toList();
            }
            return Collections.emptyList();
        } catch (SQLException ex) {
            return Collections.emptyList();
        }
    }

    private JdbcMappingUtils() {
        // utility — no instances
    }
}
