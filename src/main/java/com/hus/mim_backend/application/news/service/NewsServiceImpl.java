package com.hus.mim_backend.application.news.service;

import com.hus.mim_backend.application.news.dto.*;
import com.hus.mim_backend.application.news.usecase.*;
import com.hus.mim_backend.application.port.output.*;
import java.util.List;
import java.util.UUID;

/**
 * Service orchestrating News management use cases
 */
public class NewsServiceImpl implements ManageNewsUseCase {

    private final NewsRepository newsRepository;

    public NewsServiceImpl(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    @Override
    public NewsResponse createNews(UUID authorId, CreateNewsRequest request) {
        // TODO: News creation
        return null;
    }

    @Override
    public List<NewsResponse> getAllNews() {
        // TODO: List all news
        return List.of();
    }

    @Override
    public NewsResponse getNewsDetails(UUID newsId) {
        // TODO: News detail view
        return null;
    }

    @Override
    public void deleteNews(UUID newsId) {
        // TODO: Delete news
    }
}
