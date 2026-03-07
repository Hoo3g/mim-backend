package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for research category taxonomy read/write operations.
 */
public interface ResearchCategoryRepository {
    List<ResearchCategoryResponse> findActiveCategories();

    List<ResearchCategoryResponse> findAllCategories();

    Optional<ResearchCategoryResponse> findById(UUID categoryId);

    Optional<String> findActiveCategoryName(String categoryName);

    Optional<String> findCategoryNameById(UUID categoryId);

    boolean existsCategoryWithSameName(String categoryName);

    boolean existsOtherCategoryWithSameName(UUID categoryId, String categoryName);

    UUID createCategory(String categoryName, int sortOrder, boolean active);

    int updateCategory(UUID categoryId, String categoryName, int sortOrder, boolean active);

    int deactivateCategory(UUID categoryId);
}
