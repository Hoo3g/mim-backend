package com.hus.mim_backend.application.moderation.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ModerationResearchPaperLinkResponse {
    private UUID id;
    private String title;
    private String url;
}
