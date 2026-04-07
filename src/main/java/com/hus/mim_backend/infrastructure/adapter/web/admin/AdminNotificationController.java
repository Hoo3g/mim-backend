package com.hus.mim_backend.infrastructure.adapter.web.admin;

import com.hus.mim_backend.domain.shared.DomainException;
import com.hus.mim_backend.infrastructure.adapter.notification.AdminNotificationSseEmitter;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE endpoint for real-time admin notifications.
 */
@RestController
@RequestMapping(ApiEndpoints.ADMIN_NOTIFICATIONS)
public class AdminNotificationController {
    private final AdminNotificationSseEmitter sseEmitter;

    public AdminNotificationController(AdminNotificationSseEmitter sseEmitter) {
        this.sseEmitter = sseEmitter;
    }

    @GetMapping(value = ApiEndpoints.NOTIFICATIONS_STREAM, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('PERM_ADMIN_DASHBOARD_VIEW','PERM_MODERATION_POSTS_VIEW','PERM_MODERATION_POSTS_ACTION','PERM_MODERATION_PAPERS_VIEW','PERM_MODERATION_PAPERS_ACTION')")
    public SseEmitter streamNotifications(Authentication authentication) {
        String email = resolveAuthenticatedEmail(authentication);
        return sseEmitter.addEmitter(email);
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new DomainException("Authentication required");
        }
        String email = String.valueOf(authentication.getPrincipal());
        if (!StringUtils.hasText(email)) {
            throw new DomainException("Authentication required");
        }
        return email;
    }
}
