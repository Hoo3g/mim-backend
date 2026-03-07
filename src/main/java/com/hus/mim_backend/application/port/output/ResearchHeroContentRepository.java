package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.application.content.dto.ResearchHeroContentResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for research hero content operations.
 */
public interface ResearchHeroContentRepository {
    Optional<ResearchHeroContentResponse> findByPageKey(String pageKey);

    void upsertByPageKey(String pageKey, String titlePrefix, String titleHighlight, String subtitle, String imageUrl, UUID updatedBy);

    Optional<UUID> findUserIdByEmail(String email);
}
