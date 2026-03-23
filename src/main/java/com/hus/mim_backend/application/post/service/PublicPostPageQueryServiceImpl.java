package com.hus.mim_backend.application.post.service;

import com.hus.mim_backend.application.port.output.PublicPostPageRepository;
import com.hus.mim_backend.application.post.dto.PublicPostResponse;
import com.hus.mim_backend.application.post.usecase.QueryPublicPostsPageUseCase;
import com.hus.mim_backend.application.shared.PagedResult;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Application service for paged public post query use cases.
 */
public class PublicPostPageQueryServiceImpl implements QueryPublicPostsPageUseCase {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Map<String, List<String>> CATEGORY_ALIAS_MAP = Map.of(
            "backend", List.of("back end", "server side"),
            "frontend", List.of("front end", "ui"),
            "fullstack", List.of("full stack"),
            "ai", List.of("artificial intelligence", "machine learning", "ml"),
            "mobile", List.of("android", "ios", "flutter", "react native"),
            "game", List.of("game dev", "unity", "unreal"),
            "data", List.of("data engineer", "data analyst", "data science"),
            "devops", List.of("platform", "sre"),
            "qa", List.of("tester", "testing", "quality assurance"),
            "ui/ux", List.of("ui ux", "ux", "product design")
    );

    private final PublicPostPageRepository repository;

    public PublicPostPageQueryServiceImpl(PublicPostPageRepository repository) {
        this.repository = repository;
    }

    @Override
    public PagedResult<PublicPostResponse> getPostsPage(String keyword,
            String type,
            List<String> categories,
            int page,
            int size) {
        String normalizedKeyword = normalize(keyword);
        String normalizedType = normalize(type);
        List<String> normalizedCategories = normalizeDistinct(categories);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        if (safeSize == 0) {
            safeSize = DEFAULT_PAGE_SIZE;
        }

        return repository.findApprovedPostsPage(
                normalizedKeyword,
                normalizedType,
                normalizedCategories.stream()
                        .flatMap((category) -> buildCategoryCandidates(category).stream())
                        .distinct()
                        .toList(),
                safePage,
                safeSize);
    }

    private List<String> buildCategoryCandidates(String category) {
        List<String> aliases = CATEGORY_ALIAS_MAP.getOrDefault(category, List.of());
        return normalizeDistinct(
                java.util.stream.Stream.concat(
                                java.util.stream.Stream.of(category),
                                aliases.stream())
                        .toList()
        );
    }

    private List<String> normalizeDistinct(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .filter(StringUtils::hasText)
                .flatMap((value) -> List.of(value.split(",")).stream())
                .map(String::trim)
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private String normalize(String value) {
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
