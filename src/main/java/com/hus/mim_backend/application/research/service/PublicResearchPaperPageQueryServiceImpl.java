package com.hus.mim_backend.application.research.service;

import com.hus.mim_backend.application.port.output.ResearchPaperPageRepository;
import com.hus.mim_backend.application.research.dto.PaperResponse;
import com.hus.mim_backend.application.research.usecase.QueryPublicResearchPapersPageUseCase;
import com.hus.mim_backend.application.shared.PagedResult;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Application service for paged public research paper queries.
 */
public class PublicResearchPaperPageQueryServiceImpl implements QueryPublicResearchPapersPageUseCase {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final ResearchPaperPageRepository repository;

    public PublicResearchPaperPageQueryServiceImpl(ResearchPaperPageRepository repository) {
        this.repository = repository;
    }

    @Override
    public PagedResult<PaperResponse> getPapersPage(String keyword,
            String category,
            List<String> researchAreas,
            String metricSort,
            int page,
            int size) {
        String normalizedKeyword = normalize(keyword);
        String normalizedCategory = normalize(category);
        List<String> normalizedResearchAreas = normalizeDistinct(researchAreas);
        String normalizedMetric = normalizeMetric(metricSort);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        if (safeSize == 0) {
            safeSize = DEFAULT_PAGE_SIZE;
        }

        return repository.findApprovedPapersPage(
                normalizedKeyword,
                normalizedCategory,
                normalizedResearchAreas,
                normalizedMetric,
                safePage,
                safeSize);
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
                .toList();
    }

    private String normalizeMetric(String metricSort) {
        String normalized = normalize(metricSort);
        return switch (normalized) {
            case "views", "downloads", "bookmarks" -> normalized;
            default -> "recent";
        };
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
