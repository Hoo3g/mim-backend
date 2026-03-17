package com.hus.mim_backend.application.post.service;

import com.hus.mim_backend.application.port.output.PublicPostRepository;
import com.hus.mim_backend.application.post.dto.PublicPostResponse;
import com.hus.mim_backend.application.post.usecase.QueryPublicPostsUseCase;
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
    public List<PublicPostResponse> getPosts() {
        return repository.findAllApprovedPosts();
    }

    @Override
    public List<PublicPostResponse> getPosts(String keyword, String type, List<String> specializations) {
        String normalizedKeyword = normalize(keyword);
        String normalizedType = normalize(type);
        List<String> normalizedSpecializations = normalizeDistinct(specializations);

        return repository.findAllApprovedPosts().stream()
                .filter((post) -> matchesType(post, normalizedType))
                .filter((post) -> matchesSpecialization(post, normalizedSpecializations))
                .filter((post) -> matchesKeyword(post, normalizedKeyword))
                .toList();
    }

    @Override
    public Optional<PublicPostResponse> getPostById(UUID postId) {
        return repository.findApprovedPostById(postId);
    }

    private boolean matchesType(PublicPostResponse post, String normalizedType) {
        if (!StringUtils.hasText(normalizedType)) {
            return true;
        }

        String postType = normalize(post.getPostType());
        if ("company".equals(normalizedType)) {
            return postType.contains("company");
        }
        if ("student".equals(normalizedType)) {
            return !postType.contains("company");
        }
        return true;
    }

    private boolean matchesSpecialization(PublicPostResponse post, List<String> normalizedSpecializations) {
        if (normalizedSpecializations.isEmpty()) {
            return true;
        }

        List<String> normalizedTags = (post.getTags() == null ? List.<String>of() : post.getTags()).stream()
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .toList();

        if (normalizedTags.isEmpty()) {
            return false;
        }

        return normalizedSpecializations.stream().anyMatch((specialization) -> {
            List<String> candidates = buildSpecializationCandidates(specialization);
            return normalizedTags.stream().anyMatch((tag) ->
                    candidates.stream().anyMatch((candidate) ->
                            tag.equals(candidate) || tag.contains(candidate) || candidate.contains(tag)));
        });
    }

    private boolean matchesKeyword(PublicPostResponse post, String normalizedKeyword) {
        if (!StringUtils.hasText(normalizedKeyword)) {
            return true;
        }

        String haystack = normalize(String.join(" ",
                safe(post.getTitle()),
                safe(post.getDescription()),
                safe(post.getAuthorName()),
                safe(post.getRequirements()),
                safe(post.getAchievements()),
                safe(post.getBenefits())));
        return haystack.contains(normalizedKeyword);
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
