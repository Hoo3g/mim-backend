package com.hus.mim_backend.domain.news.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
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
    private boolean pinned;
    private UUID authorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
