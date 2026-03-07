package com.hus.mim_backend.application.moderation.usecase;

import com.hus.mim_backend.application.moderation.dto.AdminModerationActionRequest;
import com.hus.mim_backend.application.moderation.dto.ModerationPaperResponse;
import com.hus.mim_backend.application.moderation.dto.ModerationPostResponse;

import java.util.List;
import java.util.UUID;

/**
 * Input port for admin moderation queue/actions.
 */
public interface AdminModerationUseCase {
    List<ModerationPostResponse> getPostsForModeration(String status);

    List<ModerationPaperResponse> getPapersForModeration(String status);

    boolean moderatePost(String moderatorEmail, UUID postId, AdminModerationActionRequest request);

    boolean moderatePaper(String moderatorEmail, UUID paperId, AdminModerationActionRequest request);
}
