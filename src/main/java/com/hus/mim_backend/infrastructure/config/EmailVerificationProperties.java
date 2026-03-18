package com.hus.mim_backend.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.notification.email-verification")
public class EmailVerificationProperties {
    private boolean enabled = false;
    private String from;
    private String subjectPrefix = "[MIM Verify Email]";
    private String frontendBaseUrl = "http://localhost:4200";
    private long tokenTtlMinutes = 1440;
}
