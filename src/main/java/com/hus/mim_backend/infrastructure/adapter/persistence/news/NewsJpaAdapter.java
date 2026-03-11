package com.hus.mim_backend.infrastructure.adapter.persistence.news;

import com.hus.mim_backend.application.port.output.NewsRepository;
import com.hus.mim_backend.domain.news.model.News;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class NewsJpaAdapter implements NewsRepository {
    private static final String STATUS_PUBLISHED = "PUBLISHED";

    private final NewsJpaRepository repository;

    public NewsJpaAdapter(NewsJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<News> findById(UUID id) {
        return repository.findById(id).map(NewsEntity::toDomain);
    }

    @Override
    public List<News> findPublishedOrderByPinnedAndCreatedAtDesc() {
        return repository.findByStatusOrderByPinnedDescCreatedAtDesc(STATUS_PUBLISHED).stream()
                .map(NewsEntity::toDomain)
                .toList();
    }

    @Override
    public List<News> findAllOrderByCreatedAtDesc() {
        return repository.findAllForAdminOrder().stream()
                .map(NewsEntity::toDomain)
                .toList();
    }

    @Override
    public News save(News news) {
        NewsEntity saved = repository.save(NewsEntity.fromDomain(news));
        return saved.toDomain();
    }

    @Override
    public int deleteById(UUID id) {
        return repository.deleteByIdReturningCount(id);
    }
}
