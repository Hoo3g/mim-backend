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

    List<News> findByAuthorId(UUID authorId);

    List<News> findAllOrderByCreatedAtDesc();

    List<News> searchByTitle(String keyword);

    News save(News news);

    void deleteById(UUID id);
}
