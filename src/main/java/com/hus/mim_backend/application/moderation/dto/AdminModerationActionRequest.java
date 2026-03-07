package com.hus.mim_backend.application.moderation.dto;

/**
 * Moderation action request DTO.
 */
public class AdminModerationActionRequest {
    private String action;
    private String comment;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
