package com.hus.mim_backend.application.research.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
public class ResearchBookmarkResponse {
    private UUID paperId;
    private String title;
    private String researchArea;
    private String category;
    private Integer publicationYear;
    private LocalDateTime savedAt;

}
