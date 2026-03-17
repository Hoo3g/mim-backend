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
    private static final Map<String, List<String>> SPECIALIZATION_ALIAS_MAP = Map.of(
            "tri tue nhan tao", List.of("ai", "artificial intelligence"),
            "khoa hoc du lieu", List.of("khdl", "data science"),
            "khoa hoc may tinh", List.of("khmt", "computer science"),
            "toan kinh te", List.of("tkt", "actuary"),
            "an ninh mang", List.of("cybersecurity", "security")
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
            key = "T(com.hus.mim_backend.infrastructure.config.CacheKeys).queryKey(#keyword, #type, #specializations)",
            sync = true)
    public List<PublicPostResponse> getPosts(String keyword, String type, List<String> specializations) {
        String normalizedKeyword = normalize(keyword);
        String normalizedType = normalize(type);
        List<String> normalizedSpecializations = normalizeDistinct(specializations);

        return repository.findApprovedPosts(
                normalizedKeyword,
                normalizedType,
                normalizedSpecializations.stream()
                        .flatMap((specialization) -> buildSpecializationCandidates(specialization).stream())
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

    private List<String> buildSpecializationCandidates(String specialization) {
        List<String> aliases = SPECIALIZATION_ALIAS_MAP.getOrDefault(specialization, List.of());
        return normalizeDistinct(
                java.util.stream.Stream.concat(
                                java.util.stream.Stream.of(specialization),
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
