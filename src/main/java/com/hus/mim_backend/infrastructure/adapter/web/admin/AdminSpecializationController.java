package com.hus.mim_backend.infrastructure.adapter.web.admin;

import com.hus.mim_backend.application.research.dto.CreateResearchCategoryRequest;
import com.hus.mim_backend.application.research.dto.ResearchCategoryResponse;
import com.hus.mim_backend.application.research.dto.UpdateResearchCategoryRequest;
import com.hus.mim_backend.application.research.usecase.ManageSpecializationUseCase;
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
 * Admin endpoints for shared specializations.
 */
@RestController
@RequestMapping(ApiEndpoints.ADMIN_SPECIALIZATIONS)
@PreAuthorize("hasAuthority('PERM_" + RbacPermissions.RESEARCH_CATEGORY_MANAGE + "') or hasRole('ADMIN')")
public class AdminSpecializationController {
    private final ManageSpecializationUseCase manageSpecializationUseCase;

    public AdminSpecializationController(ManageSpecializationUseCase manageSpecializationUseCase) {
        this.manageSpecializationUseCase = manageSpecializationUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResearchCategoryResponse>>> getAllSpecializations() {
        List<ResearchCategoryResponse> data = manageSpecializationUseCase.getAllSpecializations();
        return ResponseEntity.ok(ApiResponse.success(data, "Get specializations successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResearchCategoryResponse>> createSpecialization(
            @RequestBody CreateResearchCategoryRequest request) {
        try {
            ResearchCategoryResponse created = manageSpecializationUseCase.createSpecialization(request);
            return ResponseEntity.ok(ApiResponse.success(created, "Create specialization successfully"));
        } catch (DomainException ex) {
            if (isDuplicateError(ex)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(ex.getMessage(), "SPECIALIZATION_ALREADY_EXISTS"));
            }
            throw ex;
        }
    }

    @PutMapping(ApiEndpoints.RESEARCH_CATEGORY_BY_ID)
    public ResponseEntity<ApiResponse<ResearchCategoryResponse>> updateSpecialization(
            @PathVariable UUID categoryId,
            @RequestBody UpdateResearchCategoryRequest request) {
        try {
            Optional<ResearchCategoryResponse> updated = manageSpecializationUseCase.updateSpecialization(categoryId, request);
            if (updated.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Specialization not found", "SPECIALIZATION_NOT_FOUND"));
            }
            return ResponseEntity.ok(ApiResponse.success(updated.get(), "Update specialization successfully"));
        } catch (DomainException ex) {
            if (isDuplicateError(ex)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(ex.getMessage(), "SPECIALIZATION_ALREADY_EXISTS"));
            }
            throw ex;
        }
    }

    @DeleteMapping(ApiEndpoints.RESEARCH_CATEGORY_BY_ID)
    public ResponseEntity<ApiResponse<Void>> deactivateSpecialization(@PathVariable UUID categoryId) {
        boolean ok = manageSpecializationUseCase.deactivateSpecialization(categoryId);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Specialization not found", "SPECIALIZATION_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Deactivate specialization successfully"));
    }

    private boolean isDuplicateError(DomainException ex) {
        return ex.getMessage() != null && ex.getMessage().startsWith("Specialization already exists:");
    }
}
