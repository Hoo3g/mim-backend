package com.hus.mim_backend.infrastructure.adapter.web.content;

import com.hus.mim_backend.application.content.dto.ResearchHeroContentResponse;
import com.hus.mim_backend.application.content.dto.UpdateResearchHeroContentRequest;
import com.hus.mim_backend.application.content.usecase.ManageResearchHeroContentUseCase;
import com.hus.mim_backend.domain.shared.DomainException;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import com.hus.mim_backend.shared.constants.RbacPermissions;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public/Admin endpoints for research page hero content.
 */
@RestController
public class ResearchHeroContentController {
    private static final String AUTH_RESEARCH_HERO_EDIT = "hasAuthority('PERM_" + RbacPermissions.RESEARCH_HERO_EDIT + "')";

    private final ManageResearchHeroContentUseCase manageResearchHeroContentUseCase;

    public ResearchHeroContentController(ManageResearchHeroContentUseCase manageResearchHeroContentUseCase) {
        this.manageResearchHeroContentUseCase = manageResearchHeroContentUseCase;
    }

    @GetMapping(ApiEndpoints.CONTENT + ApiEndpoints.RESEARCH_HERO)
    public ResponseEntity<ApiResponse<ResearchHeroContentResponse>> getResearchHeroContent() {
        ResearchHeroContentResponse content = manageResearchHeroContentUseCase.getResearchHeroContent();
        return ResponseEntity.ok(ApiResponse.success(content, "Get research hero content successfully"));
    }

    @PutMapping(ApiEndpoints.ADMIN_CONTENT + ApiEndpoints.RESEARCH_HERO)
    @PreAuthorize(AUTH_RESEARCH_HERO_EDIT)
    public ResponseEntity<ApiResponse<ResearchHeroContentResponse>> updateResearchHeroContent(
            @RequestBody UpdateResearchHeroContentRequest request,
            Authentication authentication) {
        String currentEmail = resolveAuthenticatedEmail(authentication);
        ResearchHeroContentResponse content = manageResearchHeroContentUseCase.updateResearchHeroContent(currentEmail, request);
        return ResponseEntity.ok(ApiResponse.success(content, "Update research hero content successfully"));
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
