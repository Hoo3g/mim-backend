package com.hus.mim_backend.application.analytics.service;

import com.hus.mim_backend.application.analytics.dto.AdminAnalyticsOverviewResponse;
import com.hus.mim_backend.application.analytics.dto.TrackHeartbeatRequest;
import com.hus.mim_backend.application.analytics.dto.TrackPageViewRequest;
import com.hus.mim_backend.application.analytics.model.AnalyticsTrackingRecord;
import com.hus.mim_backend.application.port.output.AnalyticsRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticsServiceImplTest {

    @Test
    void getOverviewShouldComputeMonthOverMonthDelta() {
        StubAnalyticsRepository repository = new StubAnalyticsRepository();
        repository.monthlyTraffic = List.of(
                trafficPoint("2026-02", "02/2026", 100, 40),
                trafficPoint("2026-03", "03/2026", 150, 60));
        repository.routeDistribution = List.of(routeDistributionItem("RESEARCH", 120, 50));
        repository.pageViews30d = 900;
        repository.onlineUsersNow = 11;
        repository.recruitmentPosts = 7;
        repository.researchPapers = 13;
        repository.trackedVisitors24h = 20;

        AnalyticsServiceImpl service = new AnalyticsServiceImpl(repository);
        AdminAnalyticsOverviewResponse response = service.getOverview(12, 10);

        assertEquals(900, response.getKpis().getPageViews30d());
        assertEquals(11, response.getKpis().getOnlineUsersNow());
        assertEquals(7, response.getKpis().getRecruitmentPosts());
        assertEquals(20, response.getKpis().getTotalPosts());
        assertEquals(1, response.getRouteDistribution().size());
        assertEquals("RESEARCH", response.getRouteDistribution().getFirst().getRouteKey());
        assertEquals(50.0, response.getMonthOverMonthDelta().getViewsChangePercent(), 0.001);
        assertEquals(50.0, response.getMonthOverMonthDelta().getUniqueVisitorsChangePercent(), 0.001);
        assertEquals(12, repository.lastRequestedMonths);
        assertEquals(10, repository.lastRequestedOnlineWindowMinutes);
    }

    @Test
    void getOverviewShouldClampParamsAndReturnNullPercentWhenPreviousMonthIsZero() {
        StubAnalyticsRepository repository = new StubAnalyticsRepository();
        repository.monthlyTraffic = List.of(
                trafficPoint("2026-02", "02/2026", 0, 0),
                trafficPoint("2026-03", "03/2026", 10, 5));

        AnalyticsServiceImpl service = new AnalyticsServiceImpl(repository);
        AdminAnalyticsOverviewResponse response = service.getOverview(0, 0);

        assertNull(response.getMonthOverMonthDelta().getViewsChangePercent());
        assertNull(response.getMonthOverMonthDelta().getUniqueVisitorsChangePercent());
        assertEquals(1, repository.lastRequestedMonths);
        assertEquals(1, repository.lastRequestedOnlineWindowMinutes);
    }

    @Test
    void recordPageViewAndHeartbeatShouldNormalizePathAndSyncPresence() {
        StubAnalyticsRepository repository = new StubAnalyticsRepository();
        AnalyticsServiceImpl service = new AnalyticsServiceImpl(repository);

        TrackPageViewRequest pageViewRequest = new TrackPageViewRequest();
        pageViewRequest.setVisitorId("visitor-1");
        pageViewRequest.setRouteKey("home");
        pageViewRequest.setPath("/research?keyword=test#ignored");
        pageViewRequest.setReferrer("https://example.com/search?q=abc");

        service.recordPageView(pageViewRequest, true);

        assertEquals(1, repository.insertedPageViews.size());
        assertEquals(1, repository.upsertedPresence.size());
        assertEquals("/research", repository.insertedPageViews.getFirst().getPath());
        assertEquals("HOME", repository.insertedPageViews.getFirst().getRouteKey());
        assertTrue(repository.insertedPageViews.getFirst().isAuthenticated());

        TrackHeartbeatRequest heartbeatRequest = new TrackHeartbeatRequest();
        heartbeatRequest.setVisitorId("visitor-1");
        heartbeatRequest.setRouteKey("research");
        heartbeatRequest.setPath("paper/abc");

        service.recordHeartbeat(heartbeatRequest, false);

        assertEquals(2, repository.upsertedPresence.size());
        assertEquals("/paper/abc", repository.upsertedPresence.get(1).getPath());
        assertEquals("RESEARCH", repository.upsertedPresence.get(1).getRouteKey());
    }

    private static AdminAnalyticsOverviewResponse.MonthlyTrafficPoint trafficPoint(
            String monthKey,
            String monthLabel,
            long views,
            long uniqueVisitors) {
        AdminAnalyticsOverviewResponse.MonthlyTrafficPoint point = new AdminAnalyticsOverviewResponse.MonthlyTrafficPoint();
        point.setMonthKey(monthKey);
        point.setMonthLabel(monthLabel);
        point.setViews(views);
        point.setUniqueVisitors(uniqueVisitors);
        return point;
    }

    private static AdminAnalyticsOverviewResponse.RouteDistributionItem routeDistributionItem(
            String routeKey,
            long views,
            long uniqueVisitors) {
        AdminAnalyticsOverviewResponse.RouteDistributionItem item = new AdminAnalyticsOverviewResponse.RouteDistributionItem();
        item.setRouteKey(routeKey);
        item.setViews(views);
        item.setUniqueVisitors(uniqueVisitors);
        return item;
    }

    private static final class StubAnalyticsRepository implements AnalyticsRepository {
        private long pageViews30d;
        private long onlineUsersNow;
        private long trackedVisitors24h;
        private long recruitmentPosts;
        private long researchPapers;
        private List<AdminAnalyticsOverviewResponse.MonthlyTrafficPoint> monthlyTraffic = new ArrayList<>();
        private List<AdminAnalyticsOverviewResponse.RouteDistributionItem> routeDistribution = new ArrayList<>();

        private int lastRequestedMonths;
        private int lastRequestedOnlineWindowMinutes;
        private final List<AnalyticsTrackingRecord> insertedPageViews = new ArrayList<>();
        private final List<AnalyticsTrackingRecord> upsertedPresence = new ArrayList<>();

        @Override
        public void insertPageView(AnalyticsTrackingRecord record) {
            insertedPageViews.add(record);
        }

        @Override
        public void upsertPresence(AnalyticsTrackingRecord record) {
            upsertedPresence.add(record);
        }

        @Override
        public long countPageViewsLastDays(int days) {
            return pageViews30d;
        }

        @Override
        public long countOnlineVisitors(int onlineWindowMinutes) {
            lastRequestedOnlineWindowMinutes = onlineWindowMinutes;
            return onlineUsersNow;
        }

        @Override
        public long countTrackedVisitorsLastHours(int hours) {
            return trackedVisitors24h;
        }

        @Override
        public long countRecruitmentPosts() {
            return recruitmentPosts;
        }

        @Override
        public long countResearchPapers() {
            return researchPapers;
        }

        @Override
        public List<AdminAnalyticsOverviewResponse.MonthlyTrafficPoint> findMonthlyTraffic(int months) {
            lastRequestedMonths = months;
            return monthlyTraffic;
        }

        @Override
        public List<AdminAnalyticsOverviewResponse.TopPageItem> findTopPagesLastDays(int days, int limit) {
            return List.of();
        }

        @Override
        public List<AdminAnalyticsOverviewResponse.RouteDistributionItem> findRouteDistributionLastDays(int days) {
            return routeDistribution;
        }
    }
}
