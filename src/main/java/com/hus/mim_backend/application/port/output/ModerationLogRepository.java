package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.domain.moderation.model.ModerationLog;

import java.util.List;
import java.util.UUID;

/**
 * Output port for ModerationLog persistence operations
 */
public interface ModerationLogRepository {
    List<ModerationLog> findByModeratorId(UUID moderatorId);

    List<ModerationLog> findByTargetTypeAndTargetId(String targetType, UUID targetId);

    ModerationLog save(ModerationLog log);
}
