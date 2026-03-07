package com.hus.mim_backend.application.research.usecase;

import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;

import java.util.List;

/**
 * Input port for querying public research categories.
 */
public interface QueryResearchCategoryUseCase {
    List<ResearchCategoryResponse> getActiveCategories();
}
