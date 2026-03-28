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
    List<ModerationPostResponse> findPostsByStatus(String status, String keyword, int limit, int offset);

    long countPostsByStatus(String status, String keyword);

    List<ModerationPaperResponse> findPapersByStatus(String status, String keyword, int limit, int offset);

    long countPapersByStatus(String status, String keyword);

    Optional<UUID> findUserIdByEmail(String email);

    List<String> findAdminEmails();

    int updatePostModeration(UUID postId, String approvalStatus, UUID moderatorId, String moderationComment);

    int updatePaperModeration(UUID paperId, String approvalStatus, UUID moderatorId, String moderationComment);

    int deletePostById(UUID postId);

    int deletePaperById(UUID paperId);

    void insertModerationLog(UUID moderatorId, String targetType, UUID targetId, String action, String comment);
}
