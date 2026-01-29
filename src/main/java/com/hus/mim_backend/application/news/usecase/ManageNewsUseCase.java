package com.hus.mim_backend.application.news.usecase;

import com.hus.mim_backend.application.news.dto.CreateNewsRequest;
import com.hus.mim_backend.application.news.dto.NewsResponse;
import java.util.List;
import java.util.UUID;

/**
 * Input port for managing department news
 */
public interface ManageNewsUseCase {
    NewsResponse createNews(UUID authorId, CreateNewsRequest request);

    List<NewsResponse> getAllNews();

    NewsResponse getNewsDetails(UUID newsId);

    void deleteNews(UUID newsId);
}
