package com.hus.mim_backend.infrastructure.adapter.persistence.content;

import com.hus.mim_backend.application.content.dto.ResearchHeroContentResponse;
import com.hus.mim_backend.application.port.output.ResearchHeroContentRepository;
import com.hus.mim_backend.infrastructure.adapter.persistence.auth.UserJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class ResearchHeroContentJpaAdapter implements ResearchHeroContentRepository {
    private final ResearchHeroSettingsJpaRepository repository;
    private final UserJpaRepository userJpaRepository;

    public ResearchHeroContentJpaAdapter(ResearchHeroSettingsJpaRepository repository,
            UserJpaRepository userJpaRepository) {
        this.repository = repository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Optional<ResearchHeroContentResponse> findByPageKey(String pageKey) {
        return repository.findByPageKey(pageKey).map(this::toResponse);
    }

    @Override
    @Transactional
    public void upsertByPageKey(String pageKey,
            String titlePrefix,
            String titleHighlight,
            String subtitle,
            String imageUrl,
            UUID updatedBy) {
        ResearchHeroSettingsEntity entity = repository.findByPageKey(pageKey)
                .orElseGet(() -> {
                    ResearchHeroSettingsEntity created = new ResearchHeroSettingsEntity();
                    created.setId(UUID.randomUUID());
                    created.setPageKey(pageKey);
                    return created;
                });

        entity.setTitlePrefix(titlePrefix);
        entity.setTitleHighlight(titleHighlight);
        entity.setSubtitle(subtitle);
        entity.setImageUrl(imageUrl);
        entity.setUpdatedBy(updatedBy);
        entity.setUpdatedAt(LocalDateTime.now());
        repository.save(entity);
    }

    @Override
    public Optional<UUID> findUserIdByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(user -> user.getId());
    }

    private ResearchHeroContentResponse toResponse(ResearchHeroSettingsEntity entity) {
        ResearchHeroContentResponse response = new ResearchHeroContentResponse();
        response.setPageKey(entity.getPageKey());
        response.setTitlePrefix(entity.getTitlePrefix());
        response.setTitleHighlight(entity.getTitleHighlight());
        response.setSubtitle(entity.getSubtitle());
        response.setImageUrl(entity.getImageUrl());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
