package com.hus.mim_backend.infrastructure.adapter.web.research;

import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;
import com.hus.mim_backend.application.research.usecase.QueryResearchCategoryUseCase;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public endpoints for research category taxonomy.
 */
@RestController
@RequestMapping(ApiEndpoints.RESEARCH_CATEGORIES)
public class ResearchCategoryController {
    private final QueryResearchCategoryUseCase queryResearchCategoryUseCase;

    public ResearchCategoryController(QueryResearchCategoryUseCase queryResearchCategoryUseCase) {
        this.queryResearchCategoryUseCase = queryResearchCategoryUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResearchCategoryResponse>>> getActiveCategories() {
        List<ResearchCategoryResponse> categories = queryResearchCategoryUseCase.getActiveCategories();
        return ResponseEntity.ok(ApiResponse.success(categories, "Get research categories successfully"));
    }
}
