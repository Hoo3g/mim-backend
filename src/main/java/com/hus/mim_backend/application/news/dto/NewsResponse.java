package com.hus.mim_backend.application.news.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class NewsResponse {
    private UUID id;
    private String title;
    private String summary;
    private LocalDateTime createdAt;

    // TODO: Implement getters/setters
}
