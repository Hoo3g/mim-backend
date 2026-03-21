package com.hus.mim_backend.application.moderation.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Moderation paper item DTO.
 */
@Setter
@Getter
public class ModerationPaperResponse {
    private UUID id;
    private String title;
    private String category;
    private String paperAbstract;
    private String pdfUrl;
    private Integer publicationYear;
    private String journalConference;
    private String researchArea;
    private String authorName;
    private String approvalStatus;
    private List<ModerationPaperAuthorResponse> authors;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
