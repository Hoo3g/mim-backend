package com.hus.mim_backend.infrastructure.adapter.web.admin;

import com.hus.mim_backend.application.research.dto.CreateResearchCategoryRequest;
import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;
import com.hus.mim_backend.application.research.dto.UpdateResearchCategoryRequest;
import com.hus.mim_backend.application.research.usecase.ManageResearchCategoryUseCase;
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
 * Admin endpoints for research category taxonomy management.
 */
@RestController
@RequestMapping(ApiEndpoints.ADMIN_RESEARCH_CATEGORIES)
@PreAuthorize("hasAuthority('PERM_" + RbacPermissions.RESEARCH_CATEGORY_MANAGE + "') or hasRole('ADMIN')")
public class AdminResearchCategoryController {
    private final ManageResearchCategoryUseCase manageResearchCategoryUseCase;

    public AdminResearchCategoryController(ManageResearchCategoryUseCase manageResearchCategoryUseCase) {
        this.manageResearchCategoryUseCase = manageResearchCategoryUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResearchCategoryResponse>>> getAllCategories() {
        List<ResearchCategoryResponse> categories = manageResearchCategoryUseCase.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories, "Get research categories successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResearchCategoryResponse>> createCategory(
            @RequestBody CreateResearchCategoryRequest request) {
        try {
            ResearchCategoryResponse created = manageResearchCategoryUseCase.createCategory(request);
            return ResponseEntity.ok(ApiResponse.success(created, "Create research category successfully"));
        } catch (DomainException ex) {
            if (isDuplicateCategoryError(ex)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(ex.getMessage(), "CATEGORY_ALREADY_EXISTS"));
            }
            throw ex;
        }
    }

    @PutMapping(ApiEndpoints.RESEARCH_CATEGORY_BY_ID)
    public ResponseEntity<ApiResponse<ResearchCategoryResponse>> updateCategory(
            @PathVariable UUID categoryId,
            @RequestBody UpdateResearchCategoryRequest request) {
        try {
            Optional<ResearchCategoryResponse> updated = manageResearchCategoryUseCase.updateCategory(categoryId, request);
            if (updated.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Research category not found", "CATEGORY_NOT_FOUND"));
            }
            return ResponseEntity.ok(ApiResponse.success(updated.get(), "Update research category successfully"));
        } catch (DomainException ex) {
            if (isDuplicateCategoryError(ex)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(ex.getMessage(), "CATEGORY_ALREADY_EXISTS"));
            }
            throw ex;
        }
    }

    @DeleteMapping(ApiEndpoints.RESEARCH_CATEGORY_BY_ID)
    public ResponseEntity<ApiResponse<Void>> deactivateCategory(@PathVariable UUID categoryId) {
        boolean ok = manageResearchCategoryUseCase.deactivateCategory(categoryId);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Research category not found", "CATEGORY_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Deactivate research category successfully"));
    }

    private boolean isDuplicateCategoryError(DomainException ex) {
        return ex.getMessage() != null && ex.getMessage().startsWith("Category already exists:");
    }
}
