package com.hus.mim_backend.application.content.usecase;

import com.hus.mim_backend.application.content.dto.ResearchHeroContentResponse;
import com.hus.mim_backend.application.content.dto.UpdateResearchHeroContentRequest;

/**
 * Input port for research hero content management.
 */
public interface ManageResearchHeroContentUseCase {
    ResearchHeroContentResponse getResearchHeroContent();

    ResearchHeroContentResponse updateResearchHeroContent(String updatedByEmail, UpdateResearchHeroContentRequest request);
}
