package com.hus.mim_backend.domain.news.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * News aggregate - Department bulletins/news articles
 * Maps to: news table
 */
public class News {
    private UUID id;
    private String title;
    private String content;
    private String summary; // Optional short summary for list views
    private UUID authorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // TODO: Implement getters/setters
    // TODO: Implement builder pattern
    // TODO: Implement publish() domain logic
    // TODO: Implement generateSummary() if summary is empty
}
