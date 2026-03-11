package com.hus.mim_backend.application.moderation.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Moderation post item DTO.
 */
@Setter
@Getter
public class ModerationPostResponse {
    private UUID id;
    private String title;
    private String summary;
    private String authorName;
    private String approvalStatus;
    private LocalDateTime createdAt;

}
