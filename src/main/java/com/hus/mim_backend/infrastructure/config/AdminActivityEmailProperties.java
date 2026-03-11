package com.hus.mim_backend.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Settings for delegated admin activity email notifications.
 */
@Component
@ConfigurationProperties(prefix = "app.notification.admin-activity-email")
public class AdminActivityEmailProperties {
    private boolean enabled = false;
    private String from;
    private String subjectPrefix = "[MIM Admin Activity]";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSubjectPrefix() {
        return subjectPrefix;
    }

    public void setSubjectPrefix(String subjectPrefix) {
        this.subjectPrefix = subjectPrefix;
    }
}

