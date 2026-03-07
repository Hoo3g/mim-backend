package com.hus.mim_backend.application.research.usecase;

import com.hus.mim_backend.application.research.dto.CreateResearchCategoryRequest;
import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;
import com.hus.mim_backend.application.research.dto.UpdateResearchCategoryRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for managing research category taxonomy.
 */
public interface ManageResearchCategoryUseCase {
    List<ResearchCategoryResponse> getAllCategories();

    ResearchCategoryResponse createCategory(CreateResearchCategoryRequest request);

    Optional<ResearchCategoryResponse> updateCategory(UUID categoryId, UpdateResearchCategoryRequest request);

    boolean deactivateCategory(UUID categoryId);
}
