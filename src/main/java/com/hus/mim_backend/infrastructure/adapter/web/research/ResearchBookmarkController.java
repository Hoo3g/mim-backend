package com.hus.mim_backend.infrastructure.adapter.web.research;

import com.hus.mim_backend.application.research.dto.ResearchBookmarkResponse;
import com.hus.mim_backend.application.research.usecase.ResearchBookmarkUseCase;
import com.hus.mim_backend.domain.shared.DomainException;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiEndpoints.RESEARCH)
public class ResearchBookmarkController {
    private final ResearchBookmarkUseCase researchBookmarkUseCase;

    public ResearchBookmarkController(ResearchBookmarkUseCase researchBookmarkUseCase) {
        this.researchBookmarkUseCase = researchBookmarkUseCase;
    }

    @PostMapping(ApiEndpoints.RESEARCH_BOOKMARK_BY_PAPER)
    public ResponseEntity<ApiResponse<Void>> bookmarkPaper(
            @PathVariable UUID paperId,
            Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        researchBookmarkUseCase.bookmarkPaper(email, paperId);
        return ResponseEntity.ok(ApiResponse.success(null, "Paper bookmarked"));
    }

    @DeleteMapping(ApiEndpoints.RESEARCH_BOOKMARK_BY_PAPER)
    public ResponseEntity<ApiResponse<Void>> unbookmarkPaper(
            @PathVariable UUID paperId,
            Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        researchBookmarkUseCase.unbookmarkPaper(email, paperId);
        return ResponseEntity.ok(ApiResponse.success(null, "Bookmark removed"));
    }

    @GetMapping(ApiEndpoints.RESEARCH_BOOKMARK_MY)
    public ResponseEntity<ApiResponse<List<ResearchBookmarkResponse>>> getMyBookmarks(Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        List<ResearchBookmarkResponse> data = researchBookmarkUseCase.getMyBookmarks(email);
        return ResponseEntity.ok(ApiResponse.success(data, "Get bookmarked papers successfully"));
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
