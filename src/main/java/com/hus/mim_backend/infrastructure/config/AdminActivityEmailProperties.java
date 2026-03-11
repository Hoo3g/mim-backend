package com.hus.mim_backend.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Settings for delegated admin activity email notifications.
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.notification.admin-activity-email")
public class AdminActivityEmailProperties {
    private boolean enabled = false;
    private String from;
    private String subjectPrefix = "[MIM Admin Activity]";

}

