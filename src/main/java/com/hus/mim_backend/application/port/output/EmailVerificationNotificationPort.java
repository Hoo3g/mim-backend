package com.hus.mim_backend.application.port.output;

/**
 * Sends account email verification notifications.
 */
public interface EmailVerificationNotificationPort {
    void sendVerificationEmail(String recipientEmail, String token);
}
