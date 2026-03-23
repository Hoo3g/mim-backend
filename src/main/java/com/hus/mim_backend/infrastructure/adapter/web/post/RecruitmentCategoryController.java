package com.hus.mim_backend.infrastructure.adapter.web.post;

import com.hus.mim_backend.application.post.usecase.QueryRecruitmentCategoryUseCase;
import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public endpoints for recruitment categories.
 */
@RestController
@RequestMapping(ApiEndpoints.RECRUITMENT_CATEGORIES)
public class RecruitmentCategoryController {
    private final QueryRecruitmentCategoryUseCase queryRecruitmentCategoryUseCase;

    public RecruitmentCategoryController(QueryRecruitmentCategoryUseCase queryRecruitmentCategoryUseCase) {
        this.queryRecruitmentCategoryUseCase = queryRecruitmentCategoryUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResearchCategoryResponse>>> getActiveRecruitmentCategories() {
        List<ResearchCategoryResponse> data = queryRecruitmentCategoryUseCase.getActiveRecruitmentCategories();
        return ResponseEntity.ok(ApiResponse.success(data, "Get recruitment categories successfully"));
    }
}
