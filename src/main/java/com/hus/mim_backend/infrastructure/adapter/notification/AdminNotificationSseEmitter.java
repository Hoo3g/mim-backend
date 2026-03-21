package com.hus.mim_backend.infrastructure.adapter.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages SSE connections for real-time admin notifications.
 */
@Component
public class AdminNotificationSseEmitter {
    private static final Logger log = LoggerFactory.getLogger(AdminNotificationSseEmitter.class);
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Register a new SSE connection for the given admin email.
     */
    public SseEmitter addEmitter(String adminEmail) {
        // Remove existing emitter for this admin if any
        removeEmitter(adminEmail);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitter.onCompletion(() -> {
            log.debug("SSE connection completed for {}", adminEmail);
            emitters.remove(adminEmail);
        });
        emitter.onTimeout(() -> {
            log.debug("SSE connection timed out for {}", adminEmail);
            emitters.remove(adminEmail);
        });
        emitter.onError(ex -> {
            log.debug("SSE connection error for {}: {}", adminEmail, ex.getMessage());
            emitters.remove(adminEmail);
        });

        emitters.put(adminEmail, emitter);

        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\"}"));
        } catch (IOException ex) {
            log.warn("Failed to send initial SSE event to {}", adminEmail);
            emitters.remove(adminEmail);
        }

        return emitter;
    }

    /**
     * Broadcast a pending-content notification to all connected admin clients.
     */
    public void broadcast(String contentType, String contentId, String contentTitle, String authorEmail) {
        if (emitters.isEmpty()) {
            return;
        }

        String jsonData = """
                {"contentType":"%s","contentId":"%s","contentTitle":"%s","authorEmail":"%s","timestamp":%d}"""
                .formatted(
                        escapeJson(contentType),
                        escapeJson(contentId),
                        escapeJson(contentTitle),
                        escapeJson(authorEmail),
                        System.currentTimeMillis());

        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event()
                        .name("pending-content")
                        .data(jsonData));
            } catch (IOException ex) {
                log.debug("Removing dead SSE emitter for {}", entry.getKey());
                emitters.remove(entry.getKey());
            }
        }
    }

    private void removeEmitter(String adminEmail) {
        SseEmitter existing = emitters.remove(adminEmail);
        if (existing != null) {
            existing.complete();
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
