package com.hus.mim_backend.infrastructure.adapter.web.research;

import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;
import com.hus.mim_backend.application.research.usecase.QuerySpecializationUseCase;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public endpoints for shared specializations.
 */
@RestController
@RequestMapping(ApiEndpoints.SPECIALIZATIONS)
public class SpecializationController {
    private final QuerySpecializationUseCase querySpecializationUseCase;

    public SpecializationController(QuerySpecializationUseCase querySpecializationUseCase) {
        this.querySpecializationUseCase = querySpecializationUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResearchCategoryResponse>>> getActiveSpecializations() {
        List<ResearchCategoryResponse> data = querySpecializationUseCase.getActiveSpecializations();
        return ResponseEntity.ok(ApiResponse.success(data, "Get specializations successfully"));
    }
}
