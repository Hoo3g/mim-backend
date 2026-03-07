package com.hus.mim_backend.application.research.usecase;

import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;

import java.util.List;

/**
 * Input port for querying public specializations.
 */
public interface QuerySpecializationUseCase {
    List<ResearchCategoryResponse> getActiveSpecializations();
}
