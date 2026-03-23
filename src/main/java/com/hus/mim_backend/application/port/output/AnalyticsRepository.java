package com.hus.mim_backend.application.port.output;

import com.hus.mim_backend.application.analytics.dto.AdminAnalyticsOverviewResponse;
import com.hus.mim_backend.application.analytics.model.AnalyticsTrackingRecord;

import java.util.List;

public interface AnalyticsRepository {
    void insertPageView(AnalyticsTrackingRecord record);

    void upsertPresence(AnalyticsTrackingRecord record);

    long countPageViewsLastDays(int days);

    long countOnlineVisitors(int onlineWindowMinutes);

    long countTrackedVisitorsLastHours(int hours);

    long countRecruitmentPosts();

    long countResearchPapers();

    List<AdminAnalyticsOverviewResponse.MonthlyTrafficPoint> findMonthlyTraffic(int months);

    List<AdminAnalyticsOverviewResponse.TopPageItem> findTopPagesLastDays(int days, int limit);

    List<AdminAnalyticsOverviewResponse.RouteDistributionItem> findRouteDistributionLastDays(int days);
}
