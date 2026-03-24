package com.hus.mim_backend.infrastructure.adapter.notification;

import com.hus.mim_backend.application.port.output.AdminActivityNotificationPort;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * No-op adapter. Admin delegated-activity emails are disabled by design.
 */
@Component
public class SmtpAdminActivityNotificationAdapter implements AdminActivityNotificationPort {
    @Override
    public void notifyDelegatedActivity(Set<String> recipientEmails,
            String actorEmail,
            String targetType,
            UUID targetId,
            String action,
            String comment) {
        // Intentionally disabled: keep this adapter to satisfy the port and avoid
        // accidental email side effects in moderation flows.
    }
}
