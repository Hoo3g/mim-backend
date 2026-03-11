package com.hus.mim_backend.infrastructure.adapter.persistence.news;

import com.hus.mim_backend.domain.news.model.News;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "news")
public class NewsEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    private String summary;

    @Column(name = "image_url")
    private String imageUrl;

    private String status;

    private boolean pinned;

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public News toDomain() {
        News news = new News();
        news.setId(id);
        news.setTitle(title);
        news.setContent(content);
        news.setSummary(summary);
        news.setImageUrl(imageUrl);
        news.setStatus(status);
        news.setPinned(pinned);
        news.setAuthorId(authorId);
        news.setCreatedAt(createdAt);
        news.setUpdatedAt(updatedAt);
        return news;
    }

    public static NewsEntity fromDomain(News news) {
        NewsEntity entity = new NewsEntity();
        entity.setId(news.getId());
        entity.setTitle(news.getTitle());
        entity.setContent(news.getContent());
        entity.setSummary(news.getSummary());
        entity.setImageUrl(news.getImageUrl());
        entity.setStatus(news.getStatus());
        entity.setPinned(news.isPinned());
        entity.setAuthorId(news.getAuthorId());
        entity.setCreatedAt(news.getCreatedAt());
        entity.setUpdatedAt(news.getUpdatedAt());
        return entity;
    }
}
