package com.hus.mim_backend.infrastructure.config;

import java.util.List;
import java.util.UUID;

/**
 * Backward-compatible alias kept for infrastructure config classes.
 */
public final class CacheKeys {
    private CacheKeys() {
    }

    public static String queryKey(String keyword, String type, List<String> values) {
        return com.hus.mim_backend.shared.constants.CacheKeys.queryKey(keyword, type, values);
    }

    public static String idKey(UUID id) {
        return com.hus.mim_backend.shared.constants.CacheKeys.idKey(id);
    }

    public static String singleton() {
        return com.hus.mim_backend.shared.constants.CacheKeys.singleton();
    }
}
