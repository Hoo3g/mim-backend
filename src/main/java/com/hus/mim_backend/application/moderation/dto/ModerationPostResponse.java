package com.hus.mim_backend.application.moderation.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private String description;
    private String requirements;
    private String benefits;
    private String achievements;
    private String authorName;
    private String authorAvatarUrl;
    private String postType;
    private String jobType;
    private String studentCvUrl;
    private Map<String, Object> displayInfo;
    private String location;
    private String salaryRange;
    private String status;
    private String contactEmail;
    private String contactPhone;
    private List<String> tags;
    private List<ModerationResearchPaperLinkResponse> researchPaperLinks;
    private String approvalStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
