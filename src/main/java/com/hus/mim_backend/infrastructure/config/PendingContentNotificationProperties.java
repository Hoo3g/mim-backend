package com.hus.mim_backend.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Settings for pending content notification emails sent to admins.
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.notification.pending-content-email")
public class PendingContentNotificationProperties {
    private boolean enabled = false;
    private String from;
    private String subjectPrefix = "[MIM] Bài mới cần duyệt";
}
