package com.hus.mim_backend.application.analytics.service;

import com.hus.mim_backend.application.analytics.dto.AdminAnalyticsOverviewResponse;
import com.hus.mim_backend.application.analytics.dto.TrackHeartbeatRequest;
import com.hus.mim_backend.application.analytics.dto.TrackPageViewRequest;
import com.hus.mim_backend.application.analytics.model.AnalyticsTrackingRecord;
import com.hus.mim_backend.application.analytics.usecase.QueryAdminAnalyticsUseCase;
import com.hus.mim_backend.application.analytics.usecase.RecordAnalyticsTrackingUseCase;
import com.hus.mim_backend.application.port.output.AnalyticsRepository;
import com.hus.mim_backend.domain.shared.DomainException;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class AnalyticsServiceImpl implements RecordAnalyticsTrackingUseCase, QueryAdminAnalyticsUseCase {
    private static final int MIN_MONTHS = 1;
    private static final int MAX_MONTHS = 120;
    private static final int MIN_ONLINE_WINDOW_MINUTES = 1;
    private static final int MAX_ONLINE_WINDOW_MINUTES = 120;
    private static final int TOP_PAGES_LIMIT = 8;
    private static final int PAGE_VIEWS_WINDOW_DAYS = 30;
    private static final int TOP_PAGES_WINDOW_DAYS = 30;
    private static final int TRACKED_VISITOR_WINDOW_HOURS = 24;

    private static final int MAX_VISITOR_ID_LENGTH = 100;
    private static final int MAX_ROUTE_KEY_LENGTH = 120;
    private static final int MAX_PATH_LENGTH = 512;
    private static final int MAX_REFERRER_LENGTH = 512;

    private final AnalyticsRepository repository;

    public AnalyticsServiceImpl(AnalyticsRepository repository) {
        this.repository = repository;
    }

    @Override
    public void recordPageView(TrackPageViewRequest request, boolean authenticated) {
        AnalyticsTrackingRecord record = toPageViewRecord(request, authenticated);
        repository.insertPageView(record);
        repository.upsertPresence(record);
    }

    @Override
    public void recordHeartbeat(TrackHeartbeatRequest request, boolean authenticated) {
        AnalyticsTrackingRecord record = toHeartbeatRecord(request, authenticated);
        repository.upsertPresence(record);
    }

    @Override
    public AdminAnalyticsOverviewResponse getOverview(int months, int onlineWindowMinutes) {
        int normalizedMonths = clamp(months, MIN_MONTHS, MAX_MONTHS);
        int normalizedOnlineWindowMinutes = clamp(
                onlineWindowMinutes,
                MIN_ONLINE_WINDOW_MINUTES,
                MAX_ONLINE_WINDOW_MINUTES);

        long pageViews30d = repository.countPageViewsLastDays(PAGE_VIEWS_WINDOW_DAYS);
        long onlineUsersNow = repository.countOnlineVisitors(normalizedOnlineWindowMinutes);
        long recruitmentPosts = repository.countRecruitmentPosts();
        long researchPapers = repository.countResearchPapers();
        List<AdminAnalyticsOverviewResponse.MonthlyTrafficPoint> monthlyTraffic = repository.findMonthlyTraffic(normalizedMonths);
        List<AdminAnalyticsOverviewResponse.TopPageItem> topPages = repository.findTopPagesLastDays(
                TOP_PAGES_WINDOW_DAYS,
                TOP_PAGES_LIMIT);
        List<AdminAnalyticsOverviewResponse.RouteDistributionItem> routeDistribution = repository.findRouteDistributionLastDays(
                PAGE_VIEWS_WINDOW_DAYS);
        long trackedVisitors24h = repository.countTrackedVisitorsLastHours(TRACKED_VISITOR_WINDOW_HOURS);

        AdminAnalyticsOverviewResponse response = new AdminAnalyticsOverviewResponse();

        AdminAnalyticsOverviewResponse.Kpis kpis = new AdminAnalyticsOverviewResponse.Kpis();
        kpis.setPageViews30d(pageViews30d);
        kpis.setOnlineUsersNow(onlineUsersNow);
        kpis.setRecruitmentPosts(recruitmentPosts);
        kpis.setTotalPosts(recruitmentPosts + researchPapers);
        response.setKpis(kpis);

        response.setMonthlyTraffic(monthlyTraffic);
        response.setTopPages(topPages);
        response.setRouteDistribution(routeDistribution);

        AdminAnalyticsOverviewResponse.Realtime realtime = new AdminAnalyticsOverviewResponse.Realtime();
        realtime.setOnlineUsersNow(onlineUsersNow);
        realtime.setOnlineWindowMinutes(normalizedOnlineWindowMinutes);
        realtime.setTrackedVisitors24h(trackedVisitors24h);
        realtime.setLastUpdatedAt(LocalDateTime.now());
        response.setRealtime(realtime);

        response.setMonthOverMonthDelta(computeMonthOverMonthDelta(monthlyTraffic));
        return response;
    }

    private AnalyticsTrackingRecord toPageViewRecord(TrackPageViewRequest request, boolean authenticated) {
        if (request == null) {
            throw new DomainException("Request body is required");
        }

        String visitorId = normalizeVisitorId(request.getVisitorId());
        String routeKey = normalizeRouteKey(request.getRouteKey());
        String path = normalizePath(request.getPath());
        String referrer = normalizeReferrer(request.getReferrer());

        return new AnalyticsTrackingRecord(
                visitorId,
                routeKey,
                path,
                referrer,
                authenticated,
                LocalDateTime.now());
    }

    private AnalyticsTrackingRecord toHeartbeatRecord(TrackHeartbeatRequest request, boolean authenticated) {
        if (request == null) {
            throw new DomainException("Request body is required");
        }

        String visitorId = normalizeVisitorId(request.getVisitorId());
        String routeKey = normalizeRouteKey(request.getRouteKey());
        String path = normalizePath(request.getPath());

        return new AnalyticsTrackingRecord(
                visitorId,
                routeKey,
                path,
                null,
                authenticated,
                LocalDateTime.now());
    }

    private AdminAnalyticsOverviewResponse.MonthOverMonthDelta computeMonthOverMonthDelta(
            List<AdminAnalyticsOverviewResponse.MonthlyTrafficPoint> monthlyTraffic) {
        AdminAnalyticsOverviewResponse.MonthOverMonthDelta delta = new AdminAnalyticsOverviewResponse.MonthOverMonthDelta();
        if (monthlyTraffic == null || monthlyTraffic.isEmpty()) {
            delta.setViewsChangePercent(null);
            delta.setUniqueVisitorsChangePercent(null);
            return delta;
        }

        AdminAnalyticsOverviewResponse.MonthlyTrafficPoint current = monthlyTraffic.get(monthlyTraffic.size() - 1);
        AdminAnalyticsOverviewResponse.MonthlyTrafficPoint previous = monthlyTraffic.size() > 1
                ? monthlyTraffic.get(monthlyTraffic.size() - 2)
                : null;

        long currentViews = current == null ? 0L : current.getViews();
        long previousViews = previous == null ? 0L : previous.getViews();
        long currentUniqueVisitors = current == null ? 0L : current.getUniqueVisitors();
        long previousUniqueVisitors = previous == null ? 0L : previous.getUniqueVisitors();

        delta.setCurrentMonthViews(currentViews);
        delta.setPreviousMonthViews(previousViews);
        delta.setViewsChangePercent(calculatePercentChange(currentViews, previousViews));

        delta.setCurrentMonthUniqueVisitors(currentUniqueVisitors);
        delta.setPreviousMonthUniqueVisitors(previousUniqueVisitors);
        delta.setUniqueVisitorsChangePercent(calculatePercentChange(currentUniqueVisitors, previousUniqueVisitors));

        return delta;
    }

    private Double calculatePercentChange(long currentValue, long previousValue) {
        if (previousValue <= 0) {
            return null;
        }

        double delta = currentValue - previousValue;
        return (delta * 100.0) / previousValue;
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private String normalizeVisitorId(String visitorId) {
        String normalized = normalizeRequiredText(visitorId, "visitorId");
        if (normalized.length() > MAX_VISITOR_ID_LENGTH) {
            throw new DomainException("visitorId is too long");
        }
        return normalized;
    }

    private String normalizeRouteKey(String routeKey) {
        String normalized = (routeKey == null ? "" : routeKey).trim().toUpperCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)) {
            normalized = "OTHER";
        }

        if (normalized.length() > MAX_ROUTE_KEY_LENGTH) {
            normalized = normalized.substring(0, MAX_ROUTE_KEY_LENGTH);
        }

        return normalized.replaceAll("[^A-Z0-9_\\-]", "_");
    }

    private String normalizePath(String path) {
        String normalized = normalizeRequiredText(path, "path");

        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }

        int hashIndex = normalized.indexOf('#');
        if (hashIndex >= 0) {
            normalized = normalized.substring(0, hashIndex);
        }

        normalized = normalized.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.length() > MAX_PATH_LENGTH) {
            normalized = normalized.substring(0, MAX_PATH_LENGTH);
        }

        return normalized;
    }

    private String normalizeReferrer(String referrer) {
        if (!StringUtils.hasText(referrer)) {
            return null;
        }

        String normalized = referrer.trim();
        if (normalized.length() > MAX_REFERRER_LENGTH) {
            return normalized.substring(0, MAX_REFERRER_LENGTH);
        }
        return normalized;
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new DomainException(fieldName + " is required");
        }
        return value.trim();
    }
}
