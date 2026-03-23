package com.hus.mim_backend.application.analytics.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminAnalyticsOverviewResponse {
    private Kpis kpis = new Kpis();
    private List<MonthlyTrafficPoint> monthlyTraffic = new ArrayList<>();
    private List<TopPageItem> topPages = new ArrayList<>();
    private List<RouteDistributionItem> routeDistribution = new ArrayList<>();
    private Realtime realtime = new Realtime();
    private MonthOverMonthDelta monthOverMonthDelta = new MonthOverMonthDelta();

    public Kpis getKpis() {
        return kpis;
    }

    public void setKpis(Kpis kpis) {
        this.kpis = kpis;
    }

    public List<MonthlyTrafficPoint> getMonthlyTraffic() {
        return monthlyTraffic;
    }

    public void setMonthlyTraffic(List<MonthlyTrafficPoint> monthlyTraffic) {
        this.monthlyTraffic = monthlyTraffic;
    }

    public List<TopPageItem> getTopPages() {
        return topPages;
    }

    public void setTopPages(List<TopPageItem> topPages) {
        this.topPages = topPages;
    }

    public List<RouteDistributionItem> getRouteDistribution() {
        return routeDistribution;
    }

    public void setRouteDistribution(List<RouteDistributionItem> routeDistribution) {
        this.routeDistribution = routeDistribution;
    }

    public Realtime getRealtime() {
        return realtime;
    }

    public void setRealtime(Realtime realtime) {
        this.realtime = realtime;
    }

    public MonthOverMonthDelta getMonthOverMonthDelta() {
        return monthOverMonthDelta;
    }

    public void setMonthOverMonthDelta(MonthOverMonthDelta monthOverMonthDelta) {
        this.monthOverMonthDelta = monthOverMonthDelta;
    }

    public static class Kpis {
        private long pageViews30d;
        private long onlineUsersNow;
        private long totalPosts;
        private long recruitmentPosts;

        public long getPageViews30d() {
            return pageViews30d;
        }

        public void setPageViews30d(long pageViews30d) {
            this.pageViews30d = pageViews30d;
        }

        public long getOnlineUsersNow() {
            return onlineUsersNow;
        }

        public void setOnlineUsersNow(long onlineUsersNow) {
            this.onlineUsersNow = onlineUsersNow;
        }

        public long getTotalPosts() {
            return totalPosts;
        }

        public void setTotalPosts(long totalPosts) {
            this.totalPosts = totalPosts;
        }

        public long getRecruitmentPosts() {
            return recruitmentPosts;
        }

        public void setRecruitmentPosts(long recruitmentPosts) {
            this.recruitmentPosts = recruitmentPosts;
        }
    }

    public static class MonthlyTrafficPoint {
        private String monthKey;
        private String monthLabel;
        private long views;
        private long uniqueVisitors;

        public String getMonthKey() {
            return monthKey;
        }

        public void setMonthKey(String monthKey) {
            this.monthKey = monthKey;
        }

        public String getMonthLabel() {
            return monthLabel;
        }

        public void setMonthLabel(String monthLabel) {
            this.monthLabel = monthLabel;
        }

        public long getViews() {
            return views;
        }

        public void setViews(long views) {
            this.views = views;
        }

        public long getUniqueVisitors() {
            return uniqueVisitors;
        }

        public void setUniqueVisitors(long uniqueVisitors) {
            this.uniqueVisitors = uniqueVisitors;
        }
    }

    public static class TopPageItem {
        private String routeKey;
        private String path;
        private long views;
        private long uniqueVisitors;

        public String getRouteKey() {
            return routeKey;
        }

        public void setRouteKey(String routeKey) {
            this.routeKey = routeKey;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public long getViews() {
            return views;
        }

        public void setViews(long views) {
            this.views = views;
        }

        public long getUniqueVisitors() {
            return uniqueVisitors;
        }

        public void setUniqueVisitors(long uniqueVisitors) {
            this.uniqueVisitors = uniqueVisitors;
        }
    }

    public static class Realtime {
        private long onlineUsersNow;
        private int onlineWindowMinutes;
        private long trackedVisitors24h;
        private LocalDateTime lastUpdatedAt;

        public long getOnlineUsersNow() {
            return onlineUsersNow;
        }

        public void setOnlineUsersNow(long onlineUsersNow) {
            this.onlineUsersNow = onlineUsersNow;
        }

        public int getOnlineWindowMinutes() {
            return onlineWindowMinutes;
        }

        public void setOnlineWindowMinutes(int onlineWindowMinutes) {
            this.onlineWindowMinutes = onlineWindowMinutes;
        }

        public long getTrackedVisitors24h() {
            return trackedVisitors24h;
        }

        public void setTrackedVisitors24h(long trackedVisitors24h) {
            this.trackedVisitors24h = trackedVisitors24h;
        }

        public LocalDateTime getLastUpdatedAt() {
            return lastUpdatedAt;
        }

        public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
            this.lastUpdatedAt = lastUpdatedAt;
        }
    }

    public static class RouteDistributionItem {
        private String routeKey;
        private long views;
        private long uniqueVisitors;

        public String getRouteKey() {
            return routeKey;
        }

        public void setRouteKey(String routeKey) {
            this.routeKey = routeKey;
        }

        public long getViews() {
            return views;
        }

        public void setViews(long views) {
            this.views = views;
        }

        public long getUniqueVisitors() {
            return uniqueVisitors;
        }

        public void setUniqueVisitors(long uniqueVisitors) {
            this.uniqueVisitors = uniqueVisitors;
        }
    }

    public static class MonthOverMonthDelta {
        private long currentMonthViews;
        private long previousMonthViews;
        private Double viewsChangePercent;
        private long currentMonthUniqueVisitors;
        private long previousMonthUniqueVisitors;
        private Double uniqueVisitorsChangePercent;

        public long getCurrentMonthViews() {
            return currentMonthViews;
        }

        public void setCurrentMonthViews(long currentMonthViews) {
            this.currentMonthViews = currentMonthViews;
        }

        public long getPreviousMonthViews() {
            return previousMonthViews;
        }

        public void setPreviousMonthViews(long previousMonthViews) {
            this.previousMonthViews = previousMonthViews;
        }

        public Double getViewsChangePercent() {
            return viewsChangePercent;
        }

        public void setViewsChangePercent(Double viewsChangePercent) {
            this.viewsChangePercent = viewsChangePercent;
        }

        public long getCurrentMonthUniqueVisitors() {
            return currentMonthUniqueVisitors;
        }

        public void setCurrentMonthUniqueVisitors(long currentMonthUniqueVisitors) {
            this.currentMonthUniqueVisitors = currentMonthUniqueVisitors;
        }

        public long getPreviousMonthUniqueVisitors() {
            return previousMonthUniqueVisitors;
        }

        public void setPreviousMonthUniqueVisitors(long previousMonthUniqueVisitors) {
            this.previousMonthUniqueVisitors = previousMonthUniqueVisitors;
        }

        public Double getUniqueVisitorsChangePercent() {
            return uniqueVisitorsChangePercent;
        }

        public void setUniqueVisitorsChangePercent(Double uniqueVisitorsChangePercent) {
            this.uniqueVisitorsChangePercent = uniqueVisitorsChangePercent;
        }
    }
}
