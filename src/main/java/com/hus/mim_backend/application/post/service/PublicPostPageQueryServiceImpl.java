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
    private static final Map<String, List<String>> SPECIALIZATION_ALIAS_MAP = Map.of(
            "tri tue nhan tao", List.of("ai", "artificial intelligence"),
            "khoa hoc du lieu", List.of("khdl", "data science"),
            "khoa hoc may tinh", List.of("khmt", "computer science"),
            "toan kinh te", List.of("tkt", "actuary"),
            "an ninh mang", List.of("cybersecurity", "security")
    );

    private final PublicPostPageRepository repository;

    public PublicPostPageQueryServiceImpl(PublicPostPageRepository repository) {
        this.repository = repository;
    }

    @Override
    public PagedResult<PublicPostResponse> getPostsPage(String keyword,
            String type,
            List<String> specializations,
            int page,
            int size) {
        String normalizedKeyword = normalize(keyword);
        String normalizedType = normalize(type);
        List<String> normalizedSpecializations = normalizeDistinct(specializations);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        if (safeSize == 0) {
            safeSize = DEFAULT_PAGE_SIZE;
        }

        return repository.findApprovedPostsPage(
                normalizedKeyword,
                normalizedType,
                normalizedSpecializations.stream()
                        .flatMap((specialization) -> buildSpecializationCandidates(specialization).stream())
                        .distinct()
                        .toList(),
                safePage,
                safeSize);
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
