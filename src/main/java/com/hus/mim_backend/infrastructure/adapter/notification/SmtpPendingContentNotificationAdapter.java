package com.hus.mim_backend.infrastructure.adapter.notification;

import com.hus.mim_backend.application.port.output.PendingContentNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Sends SSE notification to admins when new content is submitted for moderation.
 */
@Component
public class SmtpPendingContentNotificationAdapter implements PendingContentNotificationPort {
    private static final Logger log = LoggerFactory.getLogger(SmtpPendingContentNotificationAdapter.class);

    private final AdminNotificationSseEmitter sseEmitter;

    public SmtpPendingContentNotificationAdapter(AdminNotificationSseEmitter sseEmitter) {
        this.sseEmitter = sseEmitter;
    }

    @Override
    @Async
    public void notifyNewPendingContent(String contentType, String contentId, String contentTitle, String authorEmail) {
        // Always broadcast SSE regardless of email settings
        try {
            sseEmitter.broadcast(contentType, contentId, contentTitle, authorEmail);
        } catch (RuntimeException ex) {
            log.warn("Failed to broadcast SSE notification for {} '{}'", contentType, contentTitle, ex);
        }
    }
}
