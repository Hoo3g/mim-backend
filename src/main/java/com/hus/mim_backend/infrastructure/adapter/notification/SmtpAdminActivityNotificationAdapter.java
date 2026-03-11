package com.hus.mim_backend.infrastructure.adapter.notification;

import com.hus.mim_backend.application.port.output.AdminActivityNotificationPort;
import com.hus.mim_backend.infrastructure.config.AdminActivityEmailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * SMTP adapter for delegated admin activity notifications.
 */
@Component
public class SmtpAdminActivityNotificationAdapter implements AdminActivityNotificationPort {
    private static final Logger log = LoggerFactory.getLogger(SmtpAdminActivityNotificationAdapter.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AdminActivityEmailProperties properties;

    public SmtpAdminActivityNotificationAdapter(ObjectProvider<JavaMailSender> mailSenderProvider,
            AdminActivityEmailProperties properties) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
    }

    @Override
    public void notifyDelegatedActivity(Set<String> recipientEmails,
            String actorEmail,
            String targetType,
            UUID targetId,
            String action,
            String comment) {
        if (!properties.isEnabled()) {
            return;
        }

        Set<String> recipients = normalizeRecipients(recipientEmails);
        if (recipients.isEmpty()) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Admin activity email notification is enabled but JavaMailSender is unavailable");
            return;
        }

        String normalizedActor = normalizeText(actorEmail, "unknown");
        String normalizedTargetType = normalizeText(targetType, "UNKNOWN");
        String normalizedAction = normalizeText(action, "UNKNOWN");
        String normalizedComment = StringUtils.hasText(comment) ? comment.trim() : "N/A";
        String normalizedTargetId = targetId == null ? "unknown" : targetId.toString();
        String timestamp = OffsetDateTime.now().format(TIMESTAMP_FORMATTER);

        String subject = buildSubject(normalizedTargetType, normalizedAction, normalizedTargetId);
        String body = buildBody(normalizedActor, normalizedTargetType, normalizedAction, normalizedTargetId, normalizedComment,
                timestamp);

        for (String recipient : recipients) {
            SimpleMailMessage message = new SimpleMailMessage();
            if (StringUtils.hasText(properties.getFrom())) {
                message.setFrom(properties.getFrom().trim());
            }
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        }
    }

    private Set<String> normalizeRecipients(Set<String> recipientEmails) {
        if (recipientEmails == null || recipientEmails.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String email : recipientEmails) {
            if (!StringUtils.hasText(email)) {
                continue;
            }
            normalized.add(email.trim().toLowerCase(Locale.ROOT));
        }
        return normalized;
    }

    private String normalizeText(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim();
    }

    private String buildSubject(String targetType, String action, String targetId) {
        String prefix = StringUtils.hasText(properties.getSubjectPrefix())
                ? properties.getSubjectPrefix().trim()
                : "[MIM Admin Activity]";
        return prefix + " " + action + " " + targetType + " " + targetId;
    }

    private String buildBody(String actorEmail,
            String targetType,
            String action,
            String targetId,
            String comment,
            String timestamp) {
        return """
                Delegated admin activity was handled.

                Actor: %s
                Target type: %s
                Target id: %s
                Action: %s
                Comment: %s
                Handled at: %s
                """.formatted(actorEmail, targetType, targetId, action, comment, timestamp);
    }
}

