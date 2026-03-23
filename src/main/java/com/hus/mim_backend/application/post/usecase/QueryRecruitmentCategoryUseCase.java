package com.hus.mim_backend.application.post.usecase;

import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;

import java.util.List;

public interface QueryRecruitmentCategoryUseCase {
    List<ResearchCategoryResponse> getActiveRecruitmentCategories();
}
