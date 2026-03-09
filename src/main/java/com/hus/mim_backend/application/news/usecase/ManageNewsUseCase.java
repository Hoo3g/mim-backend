package com.hus.mim_backend.application.news.usecase;

import com.hus.mim_backend.application.news.dto.CreateNewsRequest;
import com.hus.mim_backend.application.news.dto.NewsResponse;
import com.hus.mim_backend.application.news.dto.UpdateNewsRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Input port for managing department news
 */
public interface ManageNewsUseCase {
    NewsResponse createNews(UUID authorId, CreateNewsRequest request);

    Optional<NewsResponse> updateNews(UUID newsId, UpdateNewsRequest request);

    List<NewsResponse> getPublicNews();

    List<NewsResponse> getAdminNews();

    Optional<NewsResponse> getPublicNewsDetails(UUID newsId);

    Optional<NewsResponse> getAdminNewsDetails(UUID newsId);

    boolean deleteNews(UUID newsId);
}
