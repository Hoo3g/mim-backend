package com.hus.mim_backend.infrastructure.adapter.web.research;

import com.hus.mim_backend.application.research.dto.PaperResponse;
import com.hus.mim_backend.application.research.dto.UpsertPaperRequest;
import com.hus.mim_backend.application.research.usecase.ManageResearchPortalUseCase;
import com.hus.mim_backend.application.research.usecase.QueryPublicResearchPapersPageUseCase;
import com.hus.mim_backend.application.shared.PagedResult;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final QueryPublicResearchPapersPageUseCase queryPublicResearchPapersPageUseCase;

    public ResearchPaperController(ManageResearchPortalUseCase manageResearchPortalUseCase,
            QueryPublicResearchPapersPageUseCase queryPublicResearchPapersPageUseCase) {
        this.manageResearchPortalUseCase = manageResearchPortalUseCase;
        this.queryPublicResearchPapersPageUseCase = queryPublicResearchPapersPageUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaperResponse>>> getAllPapers(
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "type", required = false) String category,
            @RequestParam(name = "specialization", required = false) List<String> researchAreas) {
        List<PaperResponse> papers = manageResearchPortalUseCase.getAllApprovedPapers(keyword, category, researchAreas);
        return ResponseEntity.ok(ApiResponse.success(papers, "Get papers successfully"));
    }

    @GetMapping(ApiEndpoints.RESEARCH_PAGED)
    public ResponseEntity<ApiResponse<PagedResult<PaperResponse>>> getPagedPapers(
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "type", required = false) String category,
            @RequestParam(name = "paperType", required = false) String paperType,
            @RequestParam(name = "specialization", required = false) List<String> researchAreas,
            @RequestParam(name = "year", required = false) Integer publicationYear,
            @RequestParam(name = "metric", required = false) String metricSort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        PagedResult<PaperResponse> papers = queryPublicResearchPapersPageUseCase.getPapersPage(
                keyword,
                category,
                paperType,
                researchAreas,
                publicationYear,
                metricSort,
                page,
                size);
        return ResponseEntity.ok(ApiResponse.success(papers, "Get paged papers successfully"));
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

    @PostMapping(ApiEndpoints.RESEARCH_TRACK_VIEW)
    public ResponseEntity<ApiResponse<Void>> trackPaperView(@PathVariable UUID paperId, Authentication authentication) {
        String currentEmail = resolveAuthenticatedEmail(authentication);
        if (!manageResearchPortalUseCase.trackApprovedPaperView(currentEmail, paperId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Research paper not found", "PAPER_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "View count updated"));
    }

    @PostMapping(ApiEndpoints.RESEARCH_TRACK_DOWNLOAD)
    public ResponseEntity<ApiResponse<Void>> trackPaperDownload(@PathVariable UUID paperId, Authentication authentication) {
        String currentEmail = resolveAuthenticatedEmail(authentication);
        if (!manageResearchPortalUseCase.trackApprovedPaperDownload(currentEmail, paperId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Research paper not found", "PAPER_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Download count updated"));
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

    @DeleteMapping(ApiEndpoints.RESEARCH_BY_ID)
    @PreAuthorize(AUTH_RESEARCH_EDIT_OWN)
    public ResponseEntity<ApiResponse<Void>> deletePaper(
            @PathVariable UUID paperId,
            Authentication authentication) {
        String currentEmail = resolveAuthenticatedEmail(authentication);
        if (!manageResearchPortalUseCase.deletePaper(currentEmail, paperId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Research paper not found", "PAPER_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Delete paper successfully"));
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
