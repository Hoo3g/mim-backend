package com.hus.mim_backend.application.post.service;

import com.hus.mim_backend.application.port.output.PublicPostRepository;
import com.hus.mim_backend.application.post.dto.PublicPostResponse;
import com.hus.mim_backend.application.post.usecase.QueryPublicPostsUseCase;
import com.hus.mim_backend.infrastructure.config.CacheNames;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for public post query use cases.
 */
public class PublicPostQueryServiceImpl implements QueryPublicPostsUseCase {
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

    private final PublicPostRepository repository;

    public PublicPostQueryServiceImpl(PublicPostRepository repository) {
        this.repository = repository;
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PUBLIC_POSTS,
            key = "T(com.hus.mim_backend.infrastructure.config.CacheKeys).singleton()",
            sync = true)
    public List<PublicPostResponse> getPosts() {
        return repository.findAllApprovedPosts();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PUBLIC_POSTS,
            key = "T(com.hus.mim_backend.infrastructure.config.CacheKeys).queryKey(#keyword, #type, #categories)",
            sync = true)
    public List<PublicPostResponse> getPosts(String keyword, String type, List<String> categories) {
        String normalizedKeyword = normalize(keyword);
        String normalizedType = normalize(type);
        List<String> normalizedCategories = normalizeDistinct(categories);

        return repository.findApprovedPosts(
                normalizedKeyword,
                normalizedType,
                normalizedCategories.stream()
                        .flatMap((category) -> buildCategoryCandidates(category).stream())
                        .distinct()
                        .toList());
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PUBLIC_POST_DETAILS,
            key = "T(com.hus.mim_backend.infrastructure.config.CacheKeys).idKey(#postId)",
            unless = "#result == null || #result.isEmpty()",
            sync = true)
    public Optional<PublicPostResponse> getPostById(UUID postId) {
        return repository.findApprovedPostById(postId);
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
