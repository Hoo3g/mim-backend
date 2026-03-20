package com.hus.mim_backend.shared.constants;

/**
 * System role name constants.
 * Eliminates hardcoded role name strings across the codebase.
 */
public final class RoleNames {

    public static final String ADMIN    = "ADMIN";
    public static final String LECTURER = "LECTURER";
    public static final String COMPANY  = "COMPANY";
    public static final String STUDENT  = "STUDENT";

    /**
     * Role priority for SQL ORDER BY CASE expressions.
     * Lower number = higher priority.
     */
    public static int priority(String roleName) {
        if (roleName == null) return 99;
        return switch (roleName.trim().toUpperCase(java.util.Locale.ROOT)) {
            case ADMIN    -> 1;
            case LECTURER -> 2;
            case COMPANY  -> 3;
            case STUDENT  -> 4;
            default       -> 99;
        };
    }

    private RoleNames() {
        // utility class — no instances
    }
}
