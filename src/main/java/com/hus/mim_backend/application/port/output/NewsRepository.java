package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.domain.news.model.News;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for News persistence operations
 */
public interface NewsRepository {
    Optional<News> findById(UUID id);

    List<News> findPublishedOrderByPinnedAndCreatedAtDesc();

    List<News> findAllOrderByCreatedAtDesc();

    News save(News news);

    int deleteById(UUID id);
}
