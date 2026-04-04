package com.hus.mim_backend.domain.news.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * News aggregate - Department bulletins/news articles
 * Maps to: news table
 */
@Setter
@Getter
public class News {
    private UUID id;
    private String title;
    private String content;
    private String summary;
    private String imageUrl;
    private String status;
    private String contentType;
    private boolean pinned;
    private UUID authorId;
    private String importSourceUrl;
    private List<NewsScheduleEntry> scheduleEntries;
    private LocalDateTime importedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
