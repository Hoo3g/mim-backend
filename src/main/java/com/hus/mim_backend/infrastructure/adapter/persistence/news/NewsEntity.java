package com.hus.mim_backend.infrastructure.adapter.persistence.news;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hus.mim_backend.domain.news.model.News;
import com.hus.mim_backend.domain.news.model.NewsScheduleEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "news")
public class NewsEntity {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<NewsScheduleEntry>> SCHEDULE_ENTRY_LIST_TYPE = new TypeReference<>() {
    };

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

    @Column(name = "content_type", nullable = false)
    private String contentType;

    private boolean pinned;

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "import_source_url", columnDefinition = "TEXT")
    private String importSourceUrl;

    @Column(name = "schedule_entries_json", columnDefinition = "TEXT")
    private String scheduleEntriesJson;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;

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

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getImportSourceUrl() {
        return importSourceUrl;
    }

    public void setImportSourceUrl(String importSourceUrl) {
        this.importSourceUrl = importSourceUrl;
    }

    public String getScheduleEntriesJson() {
        return scheduleEntriesJson;
    }

    public void setScheduleEntriesJson(String scheduleEntriesJson) {
        this.scheduleEntriesJson = scheduleEntriesJson;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(LocalDateTime importedAt) {
        this.importedAt = importedAt;
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
        news.setContentType(contentType);
        news.setPinned(pinned);
        news.setAuthorId(authorId);
        news.setImportSourceUrl(importSourceUrl);
        news.setScheduleEntries(readScheduleEntries(scheduleEntriesJson));
        news.setImportedAt(importedAt);
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
        entity.setContentType(news.getContentType());
        entity.setPinned(news.isPinned());
        entity.setAuthorId(news.getAuthorId());
        entity.setImportSourceUrl(news.getImportSourceUrl());
        entity.setScheduleEntriesJson(writeScheduleEntries(news.getScheduleEntries()));
        entity.setImportedAt(news.getImportedAt());
        entity.setCreatedAt(news.getCreatedAt());
        entity.setUpdatedAt(news.getUpdatedAt());
        return entity;
    }

    private static List<NewsScheduleEntry> readScheduleEntries(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(rawValue, SCHEDULE_ENTRY_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize news schedule entries", ex);
        }
    }

    private static String writeScheduleEntries(List<NewsScheduleEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(entries);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize news schedule entries", ex);
        }
    }
}
