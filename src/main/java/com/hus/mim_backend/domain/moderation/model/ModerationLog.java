package com.hus.mim_backend.domain.moderation.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ModerationLog entity - Audit trail for moderation actions
 * Maps to: moderation_logs table
 */
@Getter
@Setter
public class ModerationLog {
    private UUID id;
    private UUID moderatorId;
    private TargetType targetType;
    private UUID targetId;
    private ModerationAction action;
    private String comment;
    private LocalDateTime createdAt;

    public ModerationLog() {
    }

    public static ModerationLogBuilder builder() {
        return new ModerationLogBuilder();
    }

    public static ModerationLog createApproval(UUID moderatorId, TargetType targetType, UUID targetId, String comment) {
        return builder()
                .id(UUID.randomUUID())
                .moderatorId(moderatorId)
                .targetType(targetType)
                .targetId(targetId)
                .action(ModerationAction.APPROVE)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static ModerationLog createRejection(UUID moderatorId, TargetType targetType, UUID targetId, String comment) {
        return builder()
                .id(UUID.randomUUID())
                .moderatorId(moderatorId)
                .targetType(targetType)
                .targetId(targetId)
                .action(ModerationAction.REJECT)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public enum TargetType {
        POST,
        PAPER,
        USER
    }

    public enum ModerationAction {
        APPROVE,
        REJECT
    }

    public static class ModerationLogBuilder {
        private final ModerationLog log = new ModerationLog();

        public ModerationLogBuilder id(UUID id) {
            log.id = id;
            return this;
        }

        public ModerationLogBuilder moderatorId(UUID moderatorId) {
            log.moderatorId = moderatorId;
            return this;
        }

        public ModerationLogBuilder targetType(TargetType targetType) {
            log.targetType = targetType;
            return this;
        }

        public ModerationLogBuilder targetId(UUID targetId) {
            log.targetId = targetId;
            return this;
        }

        public ModerationLogBuilder action(ModerationAction action) {
            log.action = action;
            return this;
        }

        public ModerationLogBuilder comment(String comment) {
            log.comment = comment;
            return this;
        }

        public ModerationLogBuilder createdAt(LocalDateTime createdAt) {
            log.createdAt = createdAt;
            return this;
        }

        public ModerationLog build() {
            return log;
        }
    }
}
