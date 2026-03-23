package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for recruitment category taxonomy read/write operations.
 */
public interface RecruitmentCategoryRepository {
    List<ResearchCategoryResponse> findActiveRecruitmentCategories();

    List<ResearchCategoryResponse> findAllRecruitmentCategories();

    Optional<ResearchCategoryResponse> findById(UUID recruitmentCategoryId);

    List<String> findActiveRecruitmentCategoryNames(List<String> recruitmentCategoryNames);

    boolean existsRecruitmentCategoryWithSameName(String recruitmentCategoryName);

    boolean existsOtherRecruitmentCategoryWithSameName(UUID recruitmentCategoryId, String recruitmentCategoryName);

    UUID createRecruitmentCategory(String recruitmentCategoryName, int sortOrder, boolean active);

    int updateRecruitmentCategory(UUID recruitmentCategoryId, String recruitmentCategoryName, int sortOrder, boolean active);

    int deleteRecruitmentCategory(UUID recruitmentCategoryId);
}
