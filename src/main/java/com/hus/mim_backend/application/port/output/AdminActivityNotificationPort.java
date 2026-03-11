package com.hus.mim_backend.application.port.output;

import java.util.Set;
import java.util.UUID;

/**
 * Output port for notifications when delegated admin activities are handled.
 */
public interface AdminActivityNotificationPort {
    void notifyDelegatedActivity(Set<String> recipientEmails,
            String actorEmail,
            String targetType,
            UUID targetId,
            String action,
            String comment);
}

