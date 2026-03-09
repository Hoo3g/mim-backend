package com.hus.mim_backend.application.news.service;

import com.hus.mim_backend.application.news.dto.CreateNewsRequest;
import com.hus.mim_backend.application.news.dto.NewsResponse;
import com.hus.mim_backend.application.news.dto.UpdateNewsRequest;
import com.hus.mim_backend.application.news.usecase.ManageNewsUseCase;
import com.hus.mim_backend.application.port.output.NewsRepository;
import com.hus.mim_backend.domain.news.model.News;
import com.hus.mim_backend.domain.shared.DomainException;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service orchestrating News management use cases.
 */
public class NewsServiceImpl implements ManageNewsUseCase {
    private static final int MAX_TITLE_LENGTH = 512;
    private static final int DEFAULT_SUMMARY_LENGTH = 220;
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private final NewsRepository newsRepository;

    public NewsServiceImpl(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    @Override
    public NewsResponse createNews(UUID authorId, CreateNewsRequest request) {
        if (request == null) {
            throw new DomainException("Request body is required");
        }

        News news = new News();
        news.setId(UUID.randomUUID());
        news.setTitle(normalizeTitle(request.getTitle()));
        news.setContent(normalizeContent(request.getContent()));
        news.setSummary(normalizeSummary(request.getSummary(), news.getContent()));
        news.setImageUrl(normalizeOptionalText(request.getImageUrl()));
        news.setStatus(normalizeStatus(request.getStatus(), STATUS_PUBLISHED));
        news.setPinned(Boolean.TRUE.equals(request.getPinned()));
        news.setAuthorId(authorId);
        news.setCreatedAt(LocalDateTime.now());
        news.setUpdatedAt(LocalDateTime.now());

        return toResponse(newsRepository.save(news));
    }

    @Override
    public Optional<NewsResponse> updateNews(UUID newsId, UpdateNewsRequest request) {
        if (newsId == null) {
            throw new DomainException("newsId is required");
        }
        if (request == null) {
            throw new DomainException("Request body is required");
        }

        Optional<News> current = newsRepository.findById(newsId);
        if (current.isEmpty()) {
            return Optional.empty();
        }

        News news = current.get();
        news.setTitle(normalizeTitle(request.getTitle()));
        news.setContent(normalizeContent(request.getContent()));
        news.setSummary(normalizeSummary(request.getSummary(), news.getContent()));
        news.setImageUrl(normalizeOptionalText(request.getImageUrl()));
        news.setStatus(normalizeStatus(request.getStatus(), news.getStatus()));
        news.setPinned(request.getPinned() == null ? news.isPinned() : request.getPinned());
        news.setUpdatedAt(LocalDateTime.now());

        return Optional.of(toResponse(newsRepository.save(news)));
    }

    @Override
    public List<NewsResponse> getPublicNews() {
        return newsRepository.findPublishedOrderByPinnedAndCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<NewsResponse> getAdminNews() {
        return newsRepository.findAllOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public Optional<NewsResponse> getPublicNewsDetails(UUID newsId) {
        if (newsId == null) {
            throw new DomainException("newsId is required");
        }

        return newsRepository.findById(newsId)
                .filter(news -> STATUS_PUBLISHED.equalsIgnoreCase(news.getStatus()))
                .map(this::toResponse);
    }

    @Override
    public Optional<NewsResponse> getAdminNewsDetails(UUID newsId) {
        if (newsId == null) {
            throw new DomainException("newsId is required");
        }

        return newsRepository.findById(newsId).map(this::toResponse);
    }

    @Override
    public boolean deleteNews(UUID newsId) {
        if (newsId == null) {
            throw new DomainException("newsId is required");
        }
        return newsRepository.deleteById(newsId) > 0;
    }

    private NewsResponse toResponse(News news) {
        NewsResponse response = new NewsResponse();
        response.setId(news.getId());
        response.setTitle(news.getTitle());
        response.setContent(news.getContent());
        response.setSummary(news.getSummary());
        response.setImageUrl(news.getImageUrl());
        response.setStatus(news.getStatus());
        response.setPinned(news.isPinned());
        response.setAuthorId(news.getAuthorId());
        response.setCreatedAt(news.getCreatedAt());
        response.setUpdatedAt(news.getUpdatedAt());
        return response;
    }

    private String normalizeTitle(String value) {
        if (!StringUtils.hasText(value)) {
            throw new DomainException("News title is required");
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > MAX_TITLE_LENGTH) {
            throw new DomainException("News title exceeds 512 characters");
        }
        return normalized;
    }

    private String normalizeContent(String value) {
        if (!StringUtils.hasText(value)) {
            throw new DomainException("News content is required");
        }
        return value.trim();
    }

    private String normalizeSummary(String value, String fallbackContent) {
        String normalized = normalizeOptionalText(value);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        return generateSummary(fallbackContent);
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeStatus(String value, String fallbackStatus) {
        String normalized = StringUtils.hasText(value) ? value.trim().toUpperCase() : fallbackStatus;
        if (!STATUS_DRAFT.equals(normalized) && !STATUS_PUBLISHED.equals(normalized)) {
            throw new DomainException("Unsupported news status. Use DRAFT or PUBLISHED.");
        }
        return normalized;
    }

    private String generateSummary(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }

        String plain = content
                .replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (plain.length() <= DEFAULT_SUMMARY_LENGTH) {
            return plain;
        }
        return plain.substring(0, DEFAULT_SUMMARY_LENGTH).trim() + "...";
    }
}
