package com.hus.mim_backend.application.moderation.service;

import com.hus.mim_backend.application.moderation.dto.AdminModerationActionRequest;
import com.hus.mim_backend.application.moderation.dto.ModerationPaperResponse;
import com.hus.mim_backend.application.moderation.dto.ModerationPostResponse;
import com.hus.mim_backend.application.moderation.usecase.AdminModerationUseCase;
import com.hus.mim_backend.application.port.output.AdminModerationRepository;
import com.hus.mim_backend.domain.shared.DomainException;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Application service for admin moderation.
 */
public class AdminModerationServiceImpl implements AdminModerationUseCase {
    private final AdminModerationRepository repository;

    public AdminModerationServiceImpl(AdminModerationRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ModerationPostResponse> getPostsForModeration(String status) {
        String normalizedStatus = normalizeApprovalStatus(status);
        return repository.findPostsByStatus(normalizedStatus);
    }

    @Override
    public List<ModerationPaperResponse> getPapersForModeration(String status) {
        String normalizedStatus = normalizeApprovalStatus(status);
        return repository.findPapersByStatus(normalizedStatus);
    }

    @Override
    public boolean moderatePost(String moderatorEmail, UUID postId, AdminModerationActionRequest request) {
        ModerationAction action = normalizeAction(request);
        UUID moderatorId = resolveModeratorId(moderatorEmail);
        String comment = action == ModerationAction.REJECT ? normalizeOptionalComment(request.getComment()) : null;

        int updated = repository.updatePostModeration(postId, action.toApprovalStatus(), moderatorId, comment);
        if (updated == 0) {
            return false;
        }

        repository.insertModerationLog(moderatorId, "POST", postId, action.toAuditAction(), comment);
        return true;
    }

    @Override
    public boolean moderatePaper(String moderatorEmail, UUID paperId, AdminModerationActionRequest request) {
        ModerationAction action = normalizeAction(request);
        UUID moderatorId = resolveModeratorId(moderatorEmail);
        String comment = action == ModerationAction.REJECT ? normalizeOptionalComment(request.getComment()) : null;

        int updated = repository.updatePaperModeration(paperId, action.toApprovalStatus(), moderatorId, comment);
        if (updated == 0) {
            return false;
        }

        repository.insertModerationLog(moderatorId, "PAPER", paperId, action.toAuditAction(), comment);
        return true;
    }

    private String normalizeApprovalStatus(String status) {
        String normalized = status == null ? "PENDING" : status.trim().toUpperCase(Locale.ROOT);
        if (!"PENDING".equals(normalized) && !"APPROVED".equals(normalized) && !"REJECTED".equals(normalized)) {
            throw new DomainException("Unsupported status. Use PENDING, APPROVED, or REJECTED.");
        }
        return normalized;
    }

    private ModerationAction normalizeAction(AdminModerationActionRequest request) {
        if (request == null || !StringUtils.hasText(request.getAction())) {
            throw new DomainException("action is required");
        }
        String normalized = request.getAction().trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "APPROVE" -> ModerationAction.APPROVE;
            case "REJECT" -> ModerationAction.REJECT;
            default -> throw new DomainException("Unsupported action. Use APPROVE or REJECT.");
        };
    }

    private String normalizeOptionalComment(String comment) {
        if (comment == null) {
            return null;
        }
        String normalized = comment.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private UUID resolveModeratorId(String moderatorEmail) {
        if (!StringUtils.hasText(moderatorEmail)) {
            throw new DomainException("Authentication required");
        }

        return repository.findUserIdByEmail(moderatorEmail.trim())
                .orElseThrow(() -> new DomainException("Authenticated user is not found"));
    }

    private enum ModerationAction {
        APPROVE,
        REJECT;

        public String toApprovalStatus() {
            return this == APPROVE ? "APPROVED" : "REJECTED";
        }

        public String toAuditAction() {
            return this == APPROVE ? "APPROVE" : "REJECT";
        }
    }
}
