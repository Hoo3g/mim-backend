package com.hus.mim_backend.application.content.service;

import com.hus.mim_backend.application.content.dto.ResearchHeroContentResponse;
import com.hus.mim_backend.application.content.dto.UpdateResearchHeroContentRequest;
import com.hus.mim_backend.application.content.usecase.ManageResearchHeroContentUseCase;
import com.hus.mim_backend.application.port.output.ResearchHeroContentRepository;
import com.hus.mim_backend.domain.shared.DomainException;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Application service for research hero content use cases.
 */
public class ResearchHeroContentServiceImpl implements ManageResearchHeroContentUseCase {
    private static final String PAGE_KEY_RESEARCH_HOME = "RESEARCH_HOME";

    private final ResearchHeroContentRepository repository;

    public ResearchHeroContentServiceImpl(ResearchHeroContentRepository repository) {
        this.repository = repository;
    }

    @Override
    public ResearchHeroContentResponse getResearchHeroContent() {
        return repository.findByPageKey(PAGE_KEY_RESEARCH_HOME)
                .orElseGet(this::defaultContent);
    }

    @Override
    public ResearchHeroContentResponse updateResearchHeroContent(String updatedByEmail,
            UpdateResearchHeroContentRequest request) {
        validateRequest(request);
        if (!StringUtils.hasText(updatedByEmail)) {
            throw new DomainException("Authentication required");
        }

        UUID updatedBy = repository.findUserIdByEmail(updatedByEmail.trim())
                .orElseThrow(() -> new DomainException("Authenticated user is not found"));

        repository.upsertByPageKey(
                PAGE_KEY_RESEARCH_HOME,
                request.getTitlePrefix().trim(),
                request.getTitleHighlight().trim(),
                request.getSubtitle().trim(),
                request.getImageUrl().trim(),
                updatedBy);

        return repository.findByPageKey(PAGE_KEY_RESEARCH_HOME)
                .orElseThrow(() -> new IllegalStateException("Research hero content was not persisted"));
    }

    private ResearchHeroContentResponse defaultContent() {
        ResearchHeroContentResponse fallback = new ResearchHeroContentResponse();
        fallback.setPageKey(PAGE_KEY_RESEARCH_HOME);
        fallback.setTitlePrefix("Nghiên cứu");
        fallback.setTitleHighlight("Đổi mới & Sáng tạo");
        fallback.setSubtitle("Nơi hội tụ những công trình nghiên cứu khoa học tiên phong của Khoa Toán - Cơ - Tin học.");
        fallback.setImageUrl("assets/faculty_building.png");
        fallback.setUpdatedAt(LocalDateTime.now());
        return fallback;
    }

    private void validateRequest(UpdateResearchHeroContentRequest request) {
        if (request == null) {
            throw new DomainException("Request body is required");
        }
        if (!StringUtils.hasText(request.getTitlePrefix())) {
            throw new DomainException("titlePrefix is required");
        }
        if (!StringUtils.hasText(request.getTitleHighlight())) {
            throw new DomainException("titleHighlight is required");
        }
        if (!StringUtils.hasText(request.getSubtitle())) {
            throw new DomainException("subtitle is required");
        }
        if (!StringUtils.hasText(request.getImageUrl())) {
            throw new DomainException("imageUrl is required");
        }
    }
}
