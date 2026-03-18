package com.hus.mim_backend.infrastructure.adapter.notification;

import com.hus.mim_backend.application.port.output.EmailVerificationNotificationPort;
import com.hus.mim_backend.infrastructure.config.EmailVerificationProperties;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class SmtpEmailVerificationNotificationAdapter implements EmailVerificationNotificationPort {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailVerificationNotificationAdapter.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailVerificationProperties properties;

    public SmtpEmailVerificationNotificationAdapter(ObjectProvider<JavaMailSender> mailSenderProvider,
            EmailVerificationProperties properties) {
        this.mailSenderProvider = mailSenderProvider;
        this.properties = properties;
    }

    @Override
    public void sendVerificationEmail(String recipientEmail, String token) {
        if (!StringUtils.hasText(recipientEmail) || !StringUtils.hasText(token)) {
            return;
        }

        String verificationLink = buildVerificationLink(token);
        log.info("Email verification link for {}: {}", recipientEmail.trim(), verificationLink);

        if (!properties.isEnabled()) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Email verification is enabled but JavaMailSender is unavailable");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            if (StringUtils.hasText(properties.getFrom())) {
                String from = properties.getFrom().trim();
                if (StringUtils.hasText(properties.getSenderName())) {
                    helper.setFrom(new InternetAddress(from, properties.getSenderName().trim()));
                } else {
                    helper.setFrom(from);
                }
            }
            helper.setTo(recipientEmail.trim());
            helper.setSubject(resolveSubject());
            helper.setText(buildBody(verificationLink), false);
            mailSender.send(message);
            log.info("Verification email sent to {}", recipientEmail.trim());
        } catch (Exception ex) {
            log.warn("Unable to send verification email to {}", recipientEmail.trim(), ex);
        }
    }

    private String buildVerificationLink(String token) {
        String baseUrl = StringUtils.hasText(properties.getFrontendBaseUrl())
                ? properties.getFrontendBaseUrl().trim()
                : "http://localhost:4200";
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBaseUrl + "/auth/verify-email?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private String resolveSubject() {
        String prefix = StringUtils.hasText(properties.getSubjectPrefix())
                ? properties.getSubjectPrefix().trim()
                : "[MIM Verify Email]";
        return prefix + " Xác thực tài khoản";
    }

    private String buildBody(String verificationLink) {
        return """
                Chao mung ban den voi cong thong tin MIM.

                De kich hoat tai khoan va bat dau dang bai, vui long xac thuc email qua lien ket duoi day:
                %s

                Neu ban khong tao tai khoan nay, hay bo qua email.
                """.formatted(verificationLink);
    }
}
