package com.hus.mim_backend.infrastructure.adapter.web.admin;

import com.hus.mim_backend.application.moderation.dto.AdminModerationActionRequest;
import com.hus.mim_backend.application.moderation.dto.ModerationPaperResponse;
import com.hus.mim_backend.application.moderation.dto.ModerationPostResponse;
import com.hus.mim_backend.application.moderation.usecase.AdminModerationUseCase;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin moderation endpoints for posts and research papers.
 */
@RestController
@RequestMapping(ApiEndpoints.ADMIN + ApiEndpoints.MODERATION)
public class AdminModerationController {
    private static final String AUTH_POSTS_VIEW = "hasAnyAuthority('PERM_" + RbacPermissions.MODERATION_POSTS_VIEW
            + "','PERM_" + RbacPermissions.MODERATION_POSTS_ACTION + "')";
    private static final String AUTH_POSTS_ACTION = "hasAuthority('PERM_" + RbacPermissions.MODERATION_POSTS_ACTION + "')";
    private static final String AUTH_PAPERS_VIEW = "hasAnyAuthority('PERM_" + RbacPermissions.MODERATION_PAPERS_VIEW
            + "','PERM_" + RbacPermissions.MODERATION_PAPERS_ACTION + "')";
    private static final String AUTH_PAPERS_ACTION = "hasAuthority('PERM_" + RbacPermissions.MODERATION_PAPERS_ACTION + "')";

    private final AdminModerationUseCase adminModerationUseCase;

    public AdminModerationController(AdminModerationUseCase adminModerationUseCase) {
        this.adminModerationUseCase = adminModerationUseCase;
    }

    @GetMapping(ApiEndpoints.MODERATION_POSTS)
    @PreAuthorize(AUTH_POSTS_VIEW)
    public ResponseEntity<ApiResponse<PagedResult<ModerationPostResponse>>> getPostsForModeration(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PagedResult<ModerationPostResponse> posts = adminModerationUseCase.getPostsForModeration(status, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(posts, "Get moderation posts successfully"));
    }

    @GetMapping(ApiEndpoints.MODERATION_PAPERS)
    @PreAuthorize(AUTH_PAPERS_VIEW)
    public ResponseEntity<ApiResponse<PagedResult<ModerationPaperResponse>>> getPapersForModeration(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PagedResult<ModerationPaperResponse> papers = adminModerationUseCase.getPapersForModeration(status, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(papers, "Get moderation papers successfully"));
    }

    @PatchMapping(ApiEndpoints.MODERATION_POST_BY_ID)
    @PreAuthorize(AUTH_POSTS_ACTION)
    public ResponseEntity<ApiResponse<Void>> moderatePost(
            @PathVariable UUID postId,
            @RequestBody AdminModerationActionRequest request,
            Authentication authentication) {
        String moderatorEmail = resolveAuthenticatedEmail(authentication);
        boolean ok = adminModerationUseCase.moderatePost(moderatorEmail, postId, request);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Post not found or no longer pending moderation", "POST_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Moderate post successfully"));
    }

    @PatchMapping(ApiEndpoints.MODERATION_PAPER_BY_ID)
    @PreAuthorize(AUTH_PAPERS_ACTION)
    public ResponseEntity<ApiResponse<Void>> moderatePaper(
            @PathVariable UUID paperId,
            @RequestBody AdminModerationActionRequest request,
            Authentication authentication) {
        String moderatorEmail = resolveAuthenticatedEmail(authentication);
        boolean ok = adminModerationUseCase.moderatePaper(moderatorEmail, paperId, request);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Paper not found or no longer pending moderation", "PAPER_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Moderate paper successfully"));
    }

    @DeleteMapping(ApiEndpoints.MODERATION_POST_BY_ID)
    @PreAuthorize(AUTH_POSTS_ACTION)
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable UUID postId,
            @RequestParam(required = false) String comment,
            Authentication authentication) {
        String moderatorEmail = resolveAuthenticatedEmail(authentication);
        boolean ok = adminModerationUseCase.deletePost(moderatorEmail, postId, comment);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Post not found", "POST_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Delete post successfully"));
    }

    @DeleteMapping(ApiEndpoints.MODERATION_PAPER_BY_ID)
    @PreAuthorize(AUTH_PAPERS_ACTION)
    public ResponseEntity<ApiResponse<Void>> deletePaper(
            @PathVariable UUID paperId,
            @RequestParam(required = false) String comment,
            Authentication authentication) {
        String moderatorEmail = resolveAuthenticatedEmail(authentication);
        boolean ok = adminModerationUseCase.deletePaper(moderatorEmail, paperId, comment);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Paper not found", "PAPER_NOT_FOUND"));
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
