package com.hus.mim_backend.application.post.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PostResponse {
    private UUID id;
    private String title;
    private String description;
    private String postType;
    private String status;
    private LocalDateTime createdAt;

    // TODO: Implement getters/setters
}
