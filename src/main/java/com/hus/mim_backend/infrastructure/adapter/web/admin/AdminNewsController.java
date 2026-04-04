package com.hus.mim_backend.infrastructure.adapter.web.admin;

import com.hus.mim_backend.application.news.dto.CreateNewsRequest;
import com.hus.mim_backend.application.news.dto.NewsResponse;
import com.hus.mim_backend.application.news.dto.NewsScheduleImportPreviewResponse;
import com.hus.mim_backend.application.news.dto.UpdateNewsRequest;
import com.hus.mim_backend.application.news.usecase.ManageNewsUseCase;
import com.hus.mim_backend.application.auth.usecase.VerifiedAccountUseCase;
import com.hus.mim_backend.domain.shared.AuthException;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import com.hus.mim_backend.shared.constants.RbacPermissions;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Admin endpoints for managing department news / bulletins.
 */
@RestController
@RequestMapping(ApiEndpoints.ADMIN_NEWS)
@PreAuthorize("hasAuthority('PERM_" + RbacPermissions.RESEARCH_HERO_EDIT + "') or hasRole('ADMIN')")
public class AdminNewsController {
    private final ManageNewsUseCase manageNewsUseCase;
    private final VerifiedAccountUseCase verifiedAccountUseCase;

    public AdminNewsController(ManageNewsUseCase manageNewsUseCase, VerifiedAccountUseCase verifiedAccountUseCase) {
        this.manageNewsUseCase = manageNewsUseCase;
        this.verifiedAccountUseCase = verifiedAccountUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NewsResponse>>> getAdminNews() {
        List<NewsResponse> data = manageNewsUseCase.getAdminNews();
        return ResponseEntity.ok(ApiResponse.success(data, "Get admin news list successfully"));
    }

    @GetMapping(ApiEndpoints.NEWS_BY_ID)
    public ResponseEntity<ApiResponse<NewsResponse>> getAdminNewsDetail(@PathVariable UUID newsId) {
        Optional<NewsResponse> news = manageNewsUseCase.getAdminNewsDetails(newsId);
        return news.map(newsResponse -> ResponseEntity.ok(ApiResponse.success(newsResponse, "Get admin news detail successfully"))).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("News not found", "NEWS_NOT_FOUND")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NewsResponse>> createNews(
            @RequestBody CreateNewsRequest request,
            Authentication authentication) {
        NewsResponse created = manageNewsUseCase.createNews(resolveVerifiedUserId(authentication), request);
        return ResponseEntity.ok(ApiResponse.success(created, "Create news successfully"));
    }

    @PostMapping(path = ApiEndpoints.ADMIN_NEWS_IMPORT_SCHEDULE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<NewsScheduleImportPreviewResponse>> importResearchSchedule(
            @RequestPart(name = "file", required = false) MultipartFile file,
            @RequestParam(name = "sourceUrl", required = false) String sourceUrl,
            Authentication authentication) {
        resolveVerifiedUserId(authentication);
        NewsScheduleImportPreviewResponse preview = manageNewsUseCase.importResearchSchedule(file, sourceUrl);
        return ResponseEntity.ok(ApiResponse.success(preview, "Import research schedule successfully"));
    }

    @PutMapping(ApiEndpoints.NEWS_BY_ID)
    public ResponseEntity<ApiResponse<NewsResponse>> updateNews(
            @PathVariable UUID newsId,
            @RequestBody UpdateNewsRequest request,
            Authentication authentication) {
        resolveVerifiedUserId(authentication);
        Optional<NewsResponse> updated = manageNewsUseCase.updateNews(newsId, request);
        return updated.map(newsResponse -> ResponseEntity.ok(ApiResponse.success(newsResponse, "Update news successfully"))).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("News not found", "NEWS_NOT_FOUND")));
    }

    @DeleteMapping(ApiEndpoints.NEWS_BY_ID)
    public ResponseEntity<ApiResponse<Void>> deleteNews(@PathVariable UUID newsId, Authentication authentication) {
        resolveVerifiedUserId(authentication);
        boolean ok = manageNewsUseCase.deleteNews(newsId);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("News not found", "NEWS_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Delete news successfully"));
    }

    private UUID resolveVerifiedUserId(Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        return verifiedAccountUseCase.requireVerifiedUserId(email);
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthException("Authentication required");
        }
        String email = String.valueOf(authentication.getPrincipal());
        if (!StringUtils.hasText(email)) {
            throw new AuthException("Authentication required");
        }
        return email.trim();
    }
}
