package com.hus.mim_backend.infrastructure.adapter.persistence.analytics;

import com.hus.mim_backend.application.analytics.dto.AdminAnalyticsOverviewResponse;
import com.hus.mim_backend.application.analytics.model.AnalyticsTrackingRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JdbcAnalyticsRepositoryTest {

    @Autowired
    private JdbcAnalyticsRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldInsertPageViewAndUpsertPresence() {
        AnalyticsTrackingRecord record = new AnalyticsTrackingRecord(
                "visitor-a",
                "HOME",
                "/",
                null,
                false,
                LocalDateTime.now());

        repository.insertPageView(record);
        repository.upsertPresence(record);

        long pageViews = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM analytics_page_views WHERE visitor_id = ?",
                Long.class,
                "visitor-a");
        long presenceRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM analytics_presence WHERE visitor_id = ?",
                Long.class,
                "visitor-a");

        assertEquals(1L, pageViews);
        assertEquals(1L, presenceRows);
    }

    @Test
    void shouldReturnExpectedAggregates() {
        insertView("visitor-1", "RECRUITMENT", "/recruitment", "date_trunc('month', CURRENT_DATE) + interval '1 day'");
        insertView("visitor-2", "RECRUITMENT", "/recruitment", "date_trunc('month', CURRENT_DATE) + interval '2 day'");
        insertView("visitor-3", "NEWS", "/news", "date_trunc('month', CURRENT_DATE) - interval '1 month' + interval '3 day'");

        insertPresence("visitor-online", "RECRUITMENT", "/recruitment", "CURRENT_TIMESTAMP - interval '2 minutes'");
        insertPresence("visitor-offline", "NEWS", "/news", "CURRENT_TIMESTAMP - interval '40 minutes'");

        insertRecruitmentPost();
        insertRecruitmentPost();
        insertResearchPaper();

        assertEquals(3L, repository.countPageViewsLastDays(30));
        assertEquals(1L, repository.countOnlineVisitors(10));
        assertEquals(3L, repository.countTrackedVisitorsLastHours(24));
        assertEquals(2L, repository.countRecruitmentPosts());
        assertEquals(1L, repository.countResearchPapers());

        List<AdminAnalyticsOverviewResponse.MonthlyTrafficPoint> traffic = repository.findMonthlyTraffic(3);
        assertEquals(3, traffic.size());
        assertEquals(0L, traffic.get(0).getViews());
        assertEquals(1L, traffic.get(1).getViews());
        assertEquals(2L, traffic.get(2).getViews());
        assertEquals(2L, traffic.get(2).getUniqueVisitors());

        List<AdminAnalyticsOverviewResponse.TopPageItem> topPages = repository.findTopPagesLastDays(30, 5);
        assertTrue(topPages.size() >= 1);
        assertEquals("/recruitment", topPages.getFirst().getPath());
        assertEquals(2L, topPages.getFirst().getViews());

        List<AdminAnalyticsOverviewResponse.RouteDistributionItem> routeDistribution = repository.findRouteDistributionLastDays(30);
        assertTrue(routeDistribution.size() >= 1);
        assertEquals("RECRUITMENT", routeDistribution.getFirst().getRouteKey());
        assertEquals(2L, routeDistribution.getFirst().getViews());
    }

    private void insertView(String visitorId, String routeKey, String path, String occurredAtExpression) {
        String sql = """
                INSERT INTO analytics_page_views (
                    visitor_id,
                    route_key,
                    path,
                    referrer,
                    is_authenticated,
                    occurred_at,
                    created_at
                )
                VALUES (?, ?, ?, NULL, FALSE, %s, CURRENT_TIMESTAMP)
                """.formatted(occurredAtExpression);
        jdbcTemplate.update(sql, visitorId, routeKey, path);
    }

    private void insertPresence(String visitorId, String routeKey, String path, String lastSeenAtExpression) {
        String sql = """
                INSERT INTO analytics_presence (
                    visitor_id,
                    route_key,
                    path,
                    is_authenticated,
                    first_seen_at,
                    last_seen_at,
                    updated_at
                )
                VALUES (?, ?, ?, FALSE, CURRENT_TIMESTAMP - interval '1 hour', %s, CURRENT_TIMESTAMP)
                """.formatted(lastSeenAtExpression);
        jdbcTemplate.update(sql, visitorId, routeKey, path);
    }

    private void insertRecruitmentPost() {
        UUID postId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO posts (id, title, description, post_type, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                postId,
                "Post " + postId,
                "Description",
                "STUDENT_SEEKING_JOB");
    }

    private void insertResearchPaper() {
        UUID paperId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO research_papers (id, title, created_at, updated_at)
                VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                paperId,
                "Paper " + paperId);
    }
}
