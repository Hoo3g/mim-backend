package com.hus.mim_backend.infrastructure.config;

import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class CacheKeys {
    private CacheKeys() {
    }

    public static String queryKey(String keyword, String type, List<String> values) {
        String normalizedKeyword = normalize(keyword);
        String normalizedType = normalize(type);
        String normalizedValues = values == null ? "" : values.stream()
                .filter(StringUtils::hasText)
                .flatMap((value) -> List.of(value.split(",")).stream())
                .map(String::trim)
                .map(CacheKeys::normalize)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .reduce((left, right) -> left + "|" + right)
                .orElse("");
        return "q=" + normalizedKeyword + ";type=" + normalizedType + ";values=" + normalizedValues;
    }

    public static String idKey(UUID id) {
        return id == null ? "null" : id.toString();
    }

    public static String singleton() {
        return "singleton";
    }

    private static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }
}
