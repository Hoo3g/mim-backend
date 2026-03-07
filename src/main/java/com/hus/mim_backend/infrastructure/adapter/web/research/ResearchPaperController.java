package com.hus.mim_backend.infrastructure.adapter.web.research;

import com.hus.mim_backend.application.research.dto.PaperResponse;
import com.hus.mim_backend.application.research.dto.UpsertPaperRequest;
import com.hus.mim_backend.application.research.usecase.ManageResearchPortalUseCase;
import com.hus.mim_backend.domain.shared.DomainException;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import com.hus.mim_backend.shared.constants.RbacPermissions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Public/private research portal endpoints.
 */
@RestController
@RequestMapping(ApiEndpoints.RESEARCH)
public class ResearchPaperController {
    private static final String AUTH_RESEARCH_CREATE = "hasAuthority('PERM_" + RbacPermissions.RESEARCH_CREATE + "')";
    private static final String AUTH_RESEARCH_EDIT_OWN = "hasAuthority('PERM_" + RbacPermissions.RESEARCH_EDIT_OWN + "')";

    private final ManageResearchPortalUseCase manageResearchPortalUseCase;

    public ResearchPaperController(ManageResearchPortalUseCase manageResearchPortalUseCase) {
        this.manageResearchPortalUseCase = manageResearchPortalUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaperResponse>>> getAllPapers() {
        List<PaperResponse> papers = manageResearchPortalUseCase.getAllApprovedPapers();
        return ResponseEntity.ok(ApiResponse.success(papers, "Get papers successfully"));
    }

    @GetMapping(ApiEndpoints.RESEARCH_MY)
    public ResponseEntity<ApiResponse<List<PaperResponse>>> getMyPapers(Authentication authentication) {
        String currentEmail = resolveAuthenticatedEmail(authentication);
        List<PaperResponse> papers = manageResearchPortalUseCase.getMyPapers(currentEmail);
        return ResponseEntity.ok(ApiResponse.success(papers, "Get my papers successfully"));
    }

    @GetMapping(ApiEndpoints.RESEARCH_BY_ID)
    public ResponseEntity<ApiResponse<PaperResponse>> getPaperById(@PathVariable UUID paperId) {
        return manageResearchPortalUseCase.getApprovedPaperById(paperId)
                .map(paper -> ResponseEntity.ok(ApiResponse.success(paper, "Get paper successfully")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Research paper not found", "PAPER_NOT_FOUND")));
    }

    @PostMapping
    @PreAuthorize(AUTH_RESEARCH_CREATE)
    public ResponseEntity<ApiResponse<PaperResponse>> createPaper(
            @RequestBody UpsertPaperRequest request,
            Authentication authentication) {
        String currentEmail = resolveAuthenticatedEmail(authentication);
        PaperResponse created = manageResearchPortalUseCase.createPaper(currentEmail, request);
        return ResponseEntity.ok(ApiResponse.success(created, "Get paper successfully"));
    }

    @PutMapping(ApiEndpoints.RESEARCH_BY_ID)
    @PreAuthorize(AUTH_RESEARCH_EDIT_OWN)
    public ResponseEntity<ApiResponse<PaperResponse>> updatePaper(
            @PathVariable UUID paperId,
            @RequestBody UpsertPaperRequest request,
            Authentication authentication) {
        String currentEmail = resolveAuthenticatedEmail(authentication);
        ManageResearchPortalUseCase.UpdatePaperResult result = manageResearchPortalUseCase
                .updatePaper(currentEmail, paperId, request);

        return switch (result.getType()) {
            case FORBIDDEN -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You do not have permission to update this paper", "FORBIDDEN"));
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Research paper not found", "PAPER_NOT_FOUND"));
            case SUCCESS -> ResponseEntity.ok(ApiResponse.success(result.getPaper(), "Get paper successfully"));
        };
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new DomainException("Authentication required");
        }
        String email = String.valueOf(authentication.getPrincipal());
        if (!StringUtils.hasText(email)) {
            throw new DomainException("Authentication required");
        }
        return email;
    }
}
