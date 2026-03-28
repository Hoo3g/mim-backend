package com.hus.mim_backend.application.moderation.usecase;

import com.hus.mim_backend.application.moderation.dto.AdminModerationActionRequest;
import com.hus.mim_backend.application.moderation.dto.ModerationPaperResponse;
import com.hus.mim_backend.application.moderation.dto.ModerationPostResponse;
import com.hus.mim_backend.application.shared.PagedResult;

import java.util.UUID;

/**
 * Input port for admin moderation queue/actions.
 */
public interface AdminModerationUseCase {
    PagedResult<ModerationPostResponse> getPostsForModeration(String status, String keyword, int page, int size);

    PagedResult<ModerationPaperResponse> getPapersForModeration(String status, String keyword, int page, int size);

    boolean moderatePost(String moderatorEmail, UUID postId, AdminModerationActionRequest request);

    boolean moderatePaper(String moderatorEmail, UUID paperId, AdminModerationActionRequest request);

    boolean deletePost(String moderatorEmail, UUID postId, String comment);

    boolean deletePaper(String moderatorEmail, UUID paperId, String comment);
}
