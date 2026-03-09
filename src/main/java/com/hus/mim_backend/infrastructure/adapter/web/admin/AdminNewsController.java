package com.hus.mim_backend.infrastructure.adapter.web.admin;

import com.hus.mim_backend.application.news.dto.CreateNewsRequest;
import com.hus.mim_backend.application.news.dto.NewsResponse;
import com.hus.mim_backend.application.news.dto.UpdateNewsRequest;
import com.hus.mim_backend.application.news.usecase.ManageNewsUseCase;
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
 * Admin endpoints for managing department news / bulletins.
 */
@RestController
@RequestMapping(ApiEndpoints.ADMIN_NEWS)
@PreAuthorize("hasAuthority('PERM_" + RbacPermissions.RESEARCH_HERO_EDIT + "') or hasRole('ADMIN')")
public class AdminNewsController {
    private final ManageNewsUseCase manageNewsUseCase;

    public AdminNewsController(ManageNewsUseCase manageNewsUseCase) {
        this.manageNewsUseCase = manageNewsUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NewsResponse>>> getAdminNews() {
        List<NewsResponse> data = manageNewsUseCase.getAdminNews();
        return ResponseEntity.ok(ApiResponse.success(data, "Get admin news list successfully"));
    }

    @GetMapping(ApiEndpoints.NEWS_BY_ID)
    public ResponseEntity<ApiResponse<NewsResponse>> getAdminNewsDetail(@PathVariable UUID newsId) {
        Optional<NewsResponse> news = manageNewsUseCase.getAdminNewsDetails(newsId);
        if (news.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("News not found", "NEWS_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(news.get(), "Get admin news detail successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NewsResponse>> createNews(@RequestBody CreateNewsRequest request) {
        NewsResponse created = manageNewsUseCase.createNews(null, request);
        return ResponseEntity.ok(ApiResponse.success(created, "Create news successfully"));
    }

    @PutMapping(ApiEndpoints.NEWS_BY_ID)
    public ResponseEntity<ApiResponse<NewsResponse>> updateNews(
            @PathVariable UUID newsId,
            @RequestBody UpdateNewsRequest request) {
        Optional<NewsResponse> updated = manageNewsUseCase.updateNews(newsId, request);
        if (updated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("News not found", "NEWS_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(updated.get(), "Update news successfully"));
    }

    @DeleteMapping(ApiEndpoints.NEWS_BY_ID)
    public ResponseEntity<ApiResponse<Void>> deleteNews(@PathVariable UUID newsId) {
        boolean ok = manageNewsUseCase.deleteNews(newsId);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("News not found", "NEWS_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Delete news successfully"));
    }
}
