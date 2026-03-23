package com.hus.mim_backend.application.analytics.model;

import java.time.LocalDateTime;

public class AnalyticsTrackingRecord {
    private final String visitorId;
    private final String routeKey;
    private final String path;
    private final String referrer;
    private final boolean authenticated;
    private final LocalDateTime occurredAt;

    public AnalyticsTrackingRecord(
            String visitorId,
            String routeKey,
            String path,
            String referrer,
            boolean authenticated,
            LocalDateTime occurredAt) {
        this.visitorId = visitorId;
        this.routeKey = routeKey;
        this.path = path;
        this.referrer = referrer;
        this.authenticated = authenticated;
        this.occurredAt = occurredAt;
    }

    public String getVisitorId() {
        return visitorId;
    }

    public String getRouteKey() {
        return routeKey;
    }

    public String getPath() {
        return path;
    }

    public String getReferrer() {
        return referrer;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
