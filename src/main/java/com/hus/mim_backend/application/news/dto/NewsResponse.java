package com.hus.mim_backend.application.news.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
public class NewsResponse {
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
    private List<NewsScheduleEntryDto> scheduleEntries;
    private LocalDateTime importedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
