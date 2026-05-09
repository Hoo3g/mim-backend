package com.hus.mim_backend.application.moderation.service;

import com.hus.mim_backend.application.moderation.dto.AdminModerationActionRequest;
import com.hus.mim_backend.application.moderation.dto.ModerationPaperResponse;
import com.hus.mim_backend.application.moderation.dto.ModerationPostResponse;
import com.hus.mim_backend.application.moderation.usecase.AdminModerationUseCase;
import com.hus.mim_backend.application.port.output.AdminActivityNotificationPort;
import com.hus.mim_backend.application.port.output.AdminModerationRepository;
import com.hus.mim_backend.application.shared.PagedResult;
import com.hus.mim_backend.domain.shared.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Application service for admin moderation.
 */
public class AdminModerationServiceImpl implements AdminModerationUseCase {
    private static final Logger log = LoggerFactory.getLogger(AdminModerationServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 50;

    private final AdminModerationRepository repository;
    private final AdminActivityNotificationPort notificationPort;

    public AdminModerationServiceImpl(AdminModerationRepository repository,
            AdminActivityNotificationPort notificationPort) {
        this.repository = repository;
        this.notificationPort = notificationPort;
    }

    @Override
    public PagedResult<ModerationPostResponse> getPostsForModeration(String status, String keyword, int page, int size) {
        String normalizedStatus = normalizeApprovalStatus(status);
        String normalizedKeyword = normalizeOptionalKeyword(keyword);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = safePage * safeSize;
        return PagedResult.of(
                repository.findPostsByStatus(normalizedStatus, normalizedKeyword, safeSize, offset),
                safePage,
                safeSize,
                repository.countPostsByStatus(normalizedStatus, normalizedKeyword));
    }

    @Override
    public PagedResult<ModerationPaperResponse> getPapersForModeration(String status, String keyword, int page, int size) {
        String normalizedStatus = normalizeApprovalStatus(status);
        String normalizedKeyword = normalizeOptionalKeyword(keyword);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = safePage * safeSize;
        return PagedResult.of(
                repository.findPapersByStatus(normalizedStatus, normalizedKeyword, safeSize, offset),
                safePage,
                safeSize,
                repository.countPapersByStatus(normalizedStatus, normalizedKeyword));
    }

    @Override
    @Transactional
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
    @Transactional
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

    @Override
    @Transactional
    public boolean deletePost(String moderatorEmail, UUID postId, String comment) {
        UUID moderatorId = resolveModeratorId(moderatorEmail);
        String normalizedComment = normalizeOptionalComment(comment);

        int deleted = repository.deletePostById(postId);
        if (deleted == 0) {
            return false;
        }

        repository.insertModerationLog(moderatorId, "POST", postId, ModerationAction.DELETE.toAuditAction(), normalizedComment);
        notifyDelegatedModerationHandled(moderatorEmail, "POST", postId, ModerationAction.DELETE, normalizedComment);
        return true;
    }

    @Override
    @Transactional
    public boolean deletePaper(String moderatorEmail, UUID paperId, String comment) {
        UUID moderatorId = resolveModeratorId(moderatorEmail);
        String normalizedComment = normalizeOptionalComment(comment);

        int deleted = repository.deletePaperById(paperId);
        if (deleted == 0) {
            return false;
        }

        repository.insertModerationLog(moderatorId, "PAPER", paperId, ModerationAction.DELETE.toAuditAction(), normalizedComment);
        notifyDelegatedModerationHandled(moderatorEmail, "PAPER", paperId, ModerationAction.DELETE, normalizedComment);
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

    private String normalizeOptionalKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
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
        REJECT,
        DELETE;

        public String toApprovalStatus() {
            return switch (this) {
                case APPROVE -> "APPROVED";
                case REJECT -> "REJECTED";
                case DELETE -> throw new IllegalStateException("DELETE action does not map to approval status");
            };
        }

        public String toAuditAction() {
            return switch (this) {
                case APPROVE -> "APPROVE";
                case REJECT -> "REJECT";
                case DELETE -> "DELETE";
            };
        }
    }
}
