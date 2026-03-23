package com.hus.mim_backend.infrastructure.adapter.web.admin;

import com.hus.mim_backend.application.post.usecase.ManageRecruitmentCategoryUseCase;
import com.hus.mim_backend.application.research.dto.CreateResearchCategoryRequest;
import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;
import com.hus.mim_backend.application.research.dto.UpdateResearchCategoryRequest;
import com.hus.mim_backend.domain.shared.DomainException;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import com.hus.mim_backend.shared.constants.RbacPermissions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Admin endpoints for recruitment categories.
 */
@RestController
@RequestMapping(ApiEndpoints.ADMIN_RECRUITMENT_CATEGORIES)
@PreAuthorize("hasAuthority('PERM_" + RbacPermissions.RESEARCH_CATEGORY_MANAGE + "') or hasRole('ADMIN')")
public class AdminRecruitmentCategoryController {
    private final ManageRecruitmentCategoryUseCase manageRecruitmentCategoryUseCase;

    public AdminRecruitmentCategoryController(ManageRecruitmentCategoryUseCase manageRecruitmentCategoryUseCase) {
        this.manageRecruitmentCategoryUseCase = manageRecruitmentCategoryUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResearchCategoryResponse>>> getAllRecruitmentCategories() {
        List<ResearchCategoryResponse> data = manageRecruitmentCategoryUseCase.getAllRecruitmentCategories();
        return ResponseEntity.ok(ApiResponse.success(data, "Get recruitment categories successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResearchCategoryResponse>> createRecruitmentCategory(
            @RequestBody CreateResearchCategoryRequest request) {
        try {
            ResearchCategoryResponse created = manageRecruitmentCategoryUseCase.createRecruitmentCategory(request);
            return ResponseEntity.ok(ApiResponse.success(created, "Create recruitment category successfully"));
        } catch (DomainException ex) {
            if (isDuplicateError(ex)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(ex.getMessage(), "RECRUITMENT_CATEGORY_ALREADY_EXISTS"));
            }
            throw ex;
        }
    }

    @PutMapping(ApiEndpoints.RESEARCH_CATEGORY_BY_ID)
    public ResponseEntity<ApiResponse<ResearchCategoryResponse>> updateRecruitmentCategory(
            @PathVariable UUID categoryId,
            @RequestBody UpdateResearchCategoryRequest request) {
        try {
            Optional<ResearchCategoryResponse> updated = manageRecruitmentCategoryUseCase
                    .updateRecruitmentCategory(categoryId, request);
            return updated
                    .map(item -> ResponseEntity.ok(ApiResponse.success(item, "Update recruitment category successfully")))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("Recruitment category not found", "RECRUITMENT_CATEGORY_NOT_FOUND")));
        } catch (DomainException ex) {
            if (isDuplicateError(ex)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(ex.getMessage(), "RECRUITMENT_CATEGORY_ALREADY_EXISTS"));
            }
            throw ex;
        }
    }

    @DeleteMapping(ApiEndpoints.RESEARCH_CATEGORY_BY_ID)
    public ResponseEntity<ApiResponse<Void>> deleteRecruitmentCategory(@PathVariable UUID categoryId) {
        boolean ok = manageRecruitmentCategoryUseCase.deleteRecruitmentCategory(categoryId);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Recruitment category not found", "RECRUITMENT_CATEGORY_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Delete recruitment category successfully"));
    }

    private boolean isDuplicateError(DomainException ex) {
        return ex.getMessage() != null && ex.getMessage().startsWith("Recruitment category already exists:");
    }
}
