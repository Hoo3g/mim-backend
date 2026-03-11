package com.hus.mim_backend.application.moderation.service;

import com.hus.mim_backend.application.moderation.dto.AdminModerationActionRequest;
import com.hus.mim_backend.application.moderation.dto.ModerationPaperResponse;
import com.hus.mim_backend.application.moderation.dto.ModerationPostResponse;
import com.hus.mim_backend.application.moderation.usecase.AdminModerationUseCase;
import com.hus.mim_backend.application.port.output.AdminActivityNotificationPort;
import com.hus.mim_backend.application.port.output.AdminModerationRepository;
import com.hus.mim_backend.domain.shared.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Application service for admin moderation.
 */
public class AdminModerationServiceImpl implements AdminModerationUseCase {
    private static final Logger log = LoggerFactory.getLogger(AdminModerationServiceImpl.class);

    private final AdminModerationRepository repository;
    private final AdminActivityNotificationPort notificationPort;

    public AdminModerationServiceImpl(AdminModerationRepository repository,
            AdminActivityNotificationPort notificationPort) {
        this.repository = repository;
        this.notificationPort = notificationPort;
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
        notifyDelegatedModerationHandled(moderatorEmail, "POST", postId, action, comment);
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
        notifyDelegatedModerationHandled(moderatorEmail, "PAPER", paperId, action, comment);
        return true;
    }

    private void notifyDelegatedModerationHandled(String moderatorEmail,
            String targetType,
            UUID targetId,
            ModerationAction action,
            String comment) {
        Set<String> recipients = new LinkedHashSet<>();
        for (String adminEmail : repository.findAdminEmails()) {
            if (StringUtils.hasText(adminEmail)) {
                recipients.add(adminEmail.trim());
            }
        }
        if (StringUtils.hasText(moderatorEmail)) {
            recipients.add(moderatorEmail.trim());
        }

        if (recipients.isEmpty()) {
            return;
        }

        try {
            notificationPort.notifyDelegatedActivity(
                    recipients,
                    moderatorEmail,
                    targetType,
                    targetId,
                    action.toAuditAction(),
                    comment);
        } catch (RuntimeException ex) {
            log.warn("Failed to send delegated admin activity notification for {} {}", targetType, targetId, ex);
        }
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
