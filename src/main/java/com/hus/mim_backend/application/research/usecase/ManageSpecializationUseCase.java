package com.hus.mim_backend.application.research.usecase;

import com.hus.mim_backend.application.research.dto.CreateResearchCategoryRequest;
import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;
import com.hus.mim_backend.application.research.dto.UpdateResearchCategoryRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for managing specializations.
 */
public interface ManageSpecializationUseCase {
    List<ResearchCategoryResponse> getAllSpecializations();

    ResearchCategoryResponse createSpecialization(CreateResearchCategoryRequest request);

    Optional<ResearchCategoryResponse> updateSpecialization(UUID specializationId, UpdateResearchCategoryRequest request);

    boolean deactivateSpecialization(UUID specializationId);
}
