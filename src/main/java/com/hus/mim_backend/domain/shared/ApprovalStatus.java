package com.hus.mim_backend.domain.shared;

/**
 * Content approval workflow status.
 * Used for both Posts and Research Papers.
 */
public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED;

    /** Default status string when the column is NULL in the database. */
    public static final String DEFAULT_SQL = "PENDING";

    /**
     * SQL fragment: COALESCE(column, 'PENDING').
     * Appended before status comparisons in native SQL queries.
     */
    public static String coalesce(String columnExpression) {
        return "COALESCE(" + columnExpression + ", 'PENDING')";
    }
}
