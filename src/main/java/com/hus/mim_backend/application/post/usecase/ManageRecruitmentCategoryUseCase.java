package com.hus.mim_backend.application.post.usecase;

import com.hus.mim_backend.application.research.dto.CreateResearchCategoryRequest;
import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;
import com.hus.mim_backend.application.research.dto.UpdateResearchCategoryRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageRecruitmentCategoryUseCase {
    List<ResearchCategoryResponse> getAllRecruitmentCategories();

    ResearchCategoryResponse createRecruitmentCategory(CreateResearchCategoryRequest request);

    Optional<ResearchCategoryResponse> updateRecruitmentCategory(UUID recruitmentCategoryId,
                                                                 UpdateResearchCategoryRequest request);

    boolean deleteRecruitmentCategory(UUID recruitmentCategoryId);
}
