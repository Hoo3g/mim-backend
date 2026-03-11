package com.hus.mim_backend.application.news.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
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
    private boolean pinned;
    private UUID authorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
