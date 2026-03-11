package com.hus.mim_backend.application.post.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PostResponse {
    private UUID id;
    private String title;
    private String description;
    private String postType;
    private String status;
    private LocalDateTime createdAt;
}
