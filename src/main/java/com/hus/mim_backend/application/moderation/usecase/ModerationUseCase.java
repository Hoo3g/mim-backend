package com.hus.mim_backend.application.moderation.usecase;

import com.hus.mim_backend.application.moderation.dto.ModerationRequest;
import java.util.UUID;

/**
 * Input port for admin moderation actions
 */
public interface ModerationUseCase {
    void approveContent(UUID moderatorId, ModerationRequest request);

    void rejectContent(UUID moderatorId, ModerationRequest request);
}
