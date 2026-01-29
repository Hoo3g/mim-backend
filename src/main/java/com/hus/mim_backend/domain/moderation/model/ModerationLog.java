package com.hus.mim_backend.domain.moderation.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ModerationLog entity - Audit trail for moderation actions
 * Maps to: moderation_logs table
 */
public class ModerationLog {
    private UUID id;
    private UUID moderatorId;
    private TargetType targetType;
    private UUID targetId;
    private ModerationAction action;
    private String comment;
    private LocalDateTime createdAt;

    // TODO: Implement getters/setters
    // TODO: Implement builder pattern
    // TODO: Implement static factory method createApproval()
    // TODO: Implement static factory method createRejection()

    public enum TargetType {
        POST,
        PAPER,
        USER
    }

    public enum ModerationAction {
        APPROVE,
        REJECT
    }
}
