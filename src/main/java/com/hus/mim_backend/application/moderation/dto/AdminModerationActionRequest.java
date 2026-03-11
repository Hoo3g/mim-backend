package com.hus.mim_backend.application.moderation.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Moderation action request DTO.
 */
@Setter
@Getter
public class AdminModerationActionRequest {
    private String action;
    private String comment;

}
