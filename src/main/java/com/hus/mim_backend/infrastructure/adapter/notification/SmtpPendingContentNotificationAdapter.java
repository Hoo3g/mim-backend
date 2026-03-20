package com.hus.mim_backend.infrastructure.adapter.notification;

import com.hus.mim_backend.application.port.output.AdminModerationRepository;
import com.hus.mim_backend.application.port.output.PendingContentNotificationPort;
import com.hus.mim_backend.infrastructure.config.PendingContentNotificationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Sends email + SSE notification to admins when new content is submitted for moderation.
 */
@Component
public class SmtpPendingContentNotificationAdapter implements PendingContentNotificationPort {
    private static final Logger log = LoggerFactory.getLogger(SmtpPendingContentNotificationAdapter.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final PendingContentNotificationProperties properties;
    private final AdminModerationRepository adminModerationRepository;
    private final AdminNotificationSseEmitter sseEmitter;

    public SmtpPendingContentNotificationAdapter(ObjectProvider<JavaMailSender> mailSenderProvider,
            PendingContentNotificationProperties properties,
            AdminModerationRepository adminModerationRepository,
            AdminNotificationSseEmitter sseEmitter) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
        this.adminModerationRepository = adminModerationRepository;
        this.sseEmitter = sseEmitter;
    }

    @Override
    @Async
    public void notifyNewPendingContent(String contentType, String contentTitle, String authorEmail) {
        // Always broadcast SSE regardless of email settings
        try {
            sseEmitter.broadcast(contentType, contentTitle, authorEmail);
        } catch (RuntimeException ex) {
            log.warn("Failed to broadcast SSE notification for {} '{}'", contentType, contentTitle, ex);
        }

        // Email notification
        if (!properties.isEnabled()) {
            return;
        }

        Set<String> recipients = resolveAdminEmails();
        if (recipients.isEmpty()) {
            log.warn("Pending content email notification is enabled but no admin emails found");
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Pending content email notification is enabled but JavaMailSender is unavailable");
            return;
        }

        String normalizedContentType = normalizeContentType(contentType);
        String normalizedTitle = StringUtils.hasText(contentTitle) ? contentTitle.trim() : "Không có tiêu đề";
        String normalizedAuthor = StringUtils.hasText(authorEmail) ? authorEmail.trim() : "unknown";
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        String subject = buildSubject(normalizedContentType, normalizedTitle);
        String body = buildBody(normalizedContentType, normalizedTitle, normalizedAuthor, timestamp);

        for (String recipient : recipients) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                if (StringUtils.hasText(properties.getFrom())) {
                    message.setFrom(properties.getFrom().trim());
                }
                message.setTo(recipient);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
            } catch (RuntimeException ex) {
                log.warn("Failed to send pending content email to {}: {}", recipient, ex.getMessage());
            }
        }
    }

    private Set<String> resolveAdminEmails() {
        List<String> adminEmails = adminModerationRepository.findAdminEmails();
        if (adminEmails == null || adminEmails.isEmpty()) {
            return Set.of();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String email : adminEmails) {
            if (StringUtils.hasText(email)) {
                normalized.add(email.trim().toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }

    private String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "NỘI DUNG";
        }
        return switch (contentType.trim().toUpperCase(Locale.ROOT)) {
            case "POST" -> "BÀI TUYỂN DỤNG";
            case "PAPER" -> "BÀI NGHIÊN CỨU";
            default -> contentType.trim();
        };
    }

    private String buildSubject(String contentType, String title) {
        String prefix = StringUtils.hasText(properties.getSubjectPrefix())
                ? properties.getSubjectPrefix().trim()
                : "[MIM] Bài mới cần duyệt";
        return prefix + " — " + contentType + ": " + truncate(title, 60);
    }

    private String buildBody(String contentType, String title, String authorEmail, String timestamp) {
        return """
                Có nội dung mới cần duyệt trên hệ thống MIM.

                Loại nội dung: %s
                Tiêu đề: %s
                Người đăng: %s
                Thời gian: %s

                Vui lòng truy cập trang quản trị để xem và duyệt nội dung.
                """.formatted(contentType, title, authorEmail, timestamp);
    }

    private String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen) + "...";
    }
}
