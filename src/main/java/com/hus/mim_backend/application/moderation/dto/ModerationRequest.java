package com.hus.mim_backend.application.moderation.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class ModerationRequest {
    private String targetType;
    private UUID targetId;
    private String comment;
}
