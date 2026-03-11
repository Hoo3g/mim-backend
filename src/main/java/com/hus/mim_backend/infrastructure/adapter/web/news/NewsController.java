package com.hus.mim_backend.infrastructure.adapter.web.news;

import com.hus.mim_backend.application.news.dto.NewsResponse;
import com.hus.mim_backend.application.news.usecase.ManageNewsUseCase;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public endpoints for department news / bulletins.
 */
@RestController
@RequestMapping(ApiEndpoints.NEWS)
public class NewsController {
    private final ManageNewsUseCase manageNewsUseCase;

    public NewsController(ManageNewsUseCase manageNewsUseCase) {
        this.manageNewsUseCase = manageNewsUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NewsResponse>>> getPublicNews() {
        List<NewsResponse> data = manageNewsUseCase.getPublicNews();
        return ResponseEntity.ok(ApiResponse.success(data, "Get news list successfully"));
    }

    @GetMapping(ApiEndpoints.NEWS_BY_ID)
    public ResponseEntity<ApiResponse<NewsResponse>> getPublicNewsDetail(@PathVariable UUID newsId) {
        Optional<NewsResponse> news = manageNewsUseCase.getPublicNewsDetails(newsId);
        return news.map(newsResponse -> ResponseEntity.ok(ApiResponse.success(newsResponse, "Get news detail successfully"))).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("News not found", "NEWS_NOT_FOUND")));
    }
}
