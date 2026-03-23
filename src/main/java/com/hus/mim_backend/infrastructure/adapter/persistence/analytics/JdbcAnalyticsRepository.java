package com.hus.mim_backend.infrastructure.adapter.persistence.analytics;

import com.hus.mim_backend.application.analytics.dto.AdminAnalyticsOverviewResponse;
import com.hus.mim_backend.application.analytics.model.AnalyticsTrackingRecord;
import com.hus.mim_backend.application.port.output.AnalyticsRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;

@Component
public class JdbcAnalyticsRepository implements AnalyticsRepository {
    private static final String INSERT_PAGE_VIEW_SQL = """
            INSERT INTO analytics_page_views (
                visitor_id,
                route_key,
                path,
                referrer,
                is_authenticated,
                occurred_at,
                created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

    private static final String UPSERT_PRESENCE_SQL = """
            INSERT INTO analytics_presence (
                visitor_id,
                route_key,
                path,
                is_authenticated,
                first_seen_at,
                last_seen_at,
                updated_at
            )
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (visitor_id) DO UPDATE SET
                route_key = EXCLUDED.route_key,
                path = EXCLUDED.path,
                is_authenticated = EXCLUDED.is_authenticated,
                last_seen_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            """;

    private static final String COUNT_PAGE_VIEWS_LAST_DAYS_SQL = """
            SELECT COUNT(*)
            FROM analytics_page_views
            WHERE occurred_at >= CURRENT_TIMESTAMP - make_interval(days => ?::int)
            """;

    private static final String COUNT_ONLINE_VISITORS_SQL = """
            SELECT COUNT(DISTINCT visitor_id)
            FROM analytics_presence
            WHERE last_seen_at >= CURRENT_TIMESTAMP - make_interval(mins => ?::int)
            """;

    private static final String COUNT_TRACKED_VISITORS_LAST_HOURS_SQL = """
            SELECT COUNT(DISTINCT visitor_id)
            FROM analytics_page_views
            WHERE occurred_at >= CURRENT_TIMESTAMP - make_interval(hours => ?::int)
            """;

    private static final String COUNT_RECRUITMENT_POSTS_SQL = """
            SELECT COUNT(*)
            FROM posts
            """;

    private static final String COUNT_RESEARCH_PAPERS_SQL = """
            SELECT COUNT(*)
            FROM research_papers
            """;

    private static final String SELECT_MONTHLY_TRAFFIC_SQL = """
            WITH month_anchor AS (
                SELECT date_trunc('month', CURRENT_DATE) - ((?::int - 1) * INTERVAL '1 month') AS start_month
            ),
            months AS (
                SELECT (SELECT start_month FROM month_anchor) + (gs.i * INTERVAL '1 month') AS month_start
                FROM generate_series(0, ?::int - 1) AS gs(i)
            )
            SELECT TO_CHAR(m.month_start, 'YYYY-MM') AS month_key,
                   TO_CHAR(m.month_start, 'MM/YYYY') AS month_label,
                   COALESCE(COUNT(v.id), 0) AS views,
                   COALESCE(COUNT(DISTINCT v.visitor_id), 0) AS unique_visitors
            FROM months m
            LEFT JOIN analytics_page_views v
              ON v.occurred_at >= m.month_start
             AND v.occurred_at < m.month_start + INTERVAL '1 month'
            GROUP BY m.month_start
            ORDER BY m.month_start
            """;

    private static final String SELECT_TOP_PAGES_LAST_DAYS_SQL = """
            SELECT route_key,
                   path,
                   COUNT(*) AS views,
                   COUNT(DISTINCT visitor_id) AS unique_visitors
            FROM analytics_page_views
            WHERE occurred_at >= CURRENT_TIMESTAMP - make_interval(days => ?::int)
            GROUP BY route_key, path
            ORDER BY views DESC, unique_visitors DESC, path ASC
            LIMIT ?
            """;

    private static final String SELECT_ROUTE_DISTRIBUTION_LAST_DAYS_SQL = """
            SELECT route_key,
                   COUNT(*) AS views,
                   COUNT(DISTINCT visitor_id) AS unique_visitors
            FROM analytics_page_views
            WHERE occurred_at >= CURRENT_TIMESTAMP - make_interval(days => ?::int)
            GROUP BY route_key
            ORDER BY views DESC, unique_visitors DESC, route_key ASC
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcAnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insertPageView(AnalyticsTrackingRecord record) {
        jdbcTemplate.update(
                INSERT_PAGE_VIEW_SQL,
                record.getVisitorId(),
                record.getRouteKey(),
                record.getPath(),
                record.getReferrer(),
                record.isAuthenticated(),
                Timestamp.valueOf(record.getOccurredAt()));
    }

    @Override
    public void upsertPresence(AnalyticsTrackingRecord record) {
        jdbcTemplate.update(
                UPSERT_PRESENCE_SQL,
                record.getVisitorId(),
                record.getRouteKey(),
                record.getPath(),
                record.isAuthenticated());
    }

    @Override
    public long countPageViewsLastDays(int days) {
        return queryForLong(COUNT_PAGE_VIEWS_LAST_DAYS_SQL, days);
    }

    @Override
    public long countOnlineVisitors(int onlineWindowMinutes) {
        return queryForLong(COUNT_ONLINE_VISITORS_SQL, onlineWindowMinutes);
    }

    @Override
    public long countTrackedVisitorsLastHours(int hours) {
        return queryForLong(COUNT_TRACKED_VISITORS_LAST_HOURS_SQL, hours);
    }

    @Override
    public long countRecruitmentPosts() {
        return queryForLong(COUNT_RECRUITMENT_POSTS_SQL);
    }

    @Override
    public long countResearchPapers() {
        return queryForLong(COUNT_RESEARCH_PAPERS_SQL);
    }

    @Override
    public List<AdminAnalyticsOverviewResponse.MonthlyTrafficPoint> findMonthlyTraffic(int months) {
        return jdbcTemplate.query(SELECT_MONTHLY_TRAFFIC_SQL, (rs, rowNum) -> {
            AdminAnalyticsOverviewResponse.MonthlyTrafficPoint point = new AdminAnalyticsOverviewResponse.MonthlyTrafficPoint();
            point.setMonthKey(rs.getString("month_key"));
            point.setMonthLabel(rs.getString("month_label"));
            point.setViews(rs.getLong("views"));
            point.setUniqueVisitors(rs.getLong("unique_visitors"));
            return point;
        }, months, months);
    }

    @Override
    public List<AdminAnalyticsOverviewResponse.TopPageItem> findTopPagesLastDays(int days, int limit) {
        return jdbcTemplate.query(SELECT_TOP_PAGES_LAST_DAYS_SQL, (rs, rowNum) -> {
            AdminAnalyticsOverviewResponse.TopPageItem item = new AdminAnalyticsOverviewResponse.TopPageItem();
            item.setRouteKey(rs.getString("route_key"));
            item.setPath(rs.getString("path"));
            item.setViews(rs.getLong("views"));
            item.setUniqueVisitors(rs.getLong("unique_visitors"));
            return item;
        }, days, limit);
    }

    @Override
    public List<AdminAnalyticsOverviewResponse.RouteDistributionItem> findRouteDistributionLastDays(int days) {
        return jdbcTemplate.query(SELECT_ROUTE_DISTRIBUTION_LAST_DAYS_SQL, (rs, rowNum) -> {
            AdminAnalyticsOverviewResponse.RouteDistributionItem item = new AdminAnalyticsOverviewResponse.RouteDistributionItem();
            item.setRouteKey(rs.getString("route_key"));
            item.setViews(rs.getLong("views"));
            item.setUniqueVisitors(rs.getLong("unique_visitors"));
            return item;
        }, days);
    }

    private long queryForLong(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }
}
