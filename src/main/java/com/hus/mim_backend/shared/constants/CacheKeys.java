package com.hus.mim_backend.shared.constants;

import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Shared cache key helpers referenced by cache SpEL expressions.
 */
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

    public static String researchPagedQueryKey(String keyword,
            String category,
            String paperType,
            List<String> researchAreas,
            Integer publicationYear,
            String metricSort,
            int page,
            int size) {
        String normalizedKeyword = normalize(keyword);
        String normalizedCategory = normalize(category);
        String normalizedPaperType = normalize(paperType);
        String normalizedMetric = normalize(metricSort);
        String normalizedResearchAreas = researchAreas == null ? "" : researchAreas.stream()
                .filter(StringUtils::hasText)
                .flatMap((value) -> List.of(value.split(",")).stream())
                .map(String::trim)
                .map(CacheKeys::normalize)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .reduce((left, right) -> left + "|" + right)
                .orElse("");

        return "q=" + normalizedKeyword
                + ";category=" + normalizedCategory
                + ";paperType=" + normalizedPaperType
                + ";areas=" + normalizedResearchAreas
                + ";year=" + (publicationYear == null ? "" : publicationYear)
                + ";metric=" + normalizedMetric
                + ";page=" + Math.max(page, 0)
                + ";size=" + Math.max(size, 1);
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
