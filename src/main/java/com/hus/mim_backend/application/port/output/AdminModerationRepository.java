package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.application.moderation.dto.ModerationPaperResponse;
import com.hus.mim_backend.application.moderation.dto.ModerationPostResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for admin moderation queue/actions.
 */
public interface AdminModerationRepository {
    List<ModerationPostResponse> findPostsByStatus(String status);

    List<ModerationPaperResponse> findPapersByStatus(String status);

    Optional<UUID> findUserIdByEmail(String email);

    int updatePostModeration(UUID postId, String approvalStatus, UUID moderatorId, String moderationComment);

    int updatePaperModeration(UUID paperId, String approvalStatus, UUID moderatorId, String moderationComment);

    void insertModerationLog(UUID moderatorId, String targetType, UUID targetId, String action, String comment);
}
