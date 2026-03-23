-- ============================================================
-- V20: Analytics tracking tables for admin dashboard
-- ============================================================

CREATE TABLE analytics_page_views (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    visitor_id VARCHAR(100) NOT NULL,
    route_key VARCHAR(120) NOT NULL,
    path VARCHAR(512) NOT NULL,
    referrer VARCHAR(512),
    is_authenticated BOOLEAN NOT NULL DEFAULT FALSE,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_analytics_page_views_occurred_at ON analytics_page_views(occurred_at DESC);
CREATE INDEX idx_analytics_page_views_visitor_id ON analytics_page_views(visitor_id);
CREATE INDEX idx_analytics_page_views_route_key ON analytics_page_views(route_key);

CREATE TABLE analytics_presence (
    visitor_id VARCHAR(100) PRIMARY KEY,
    route_key VARCHAR(120) NOT NULL,
    path VARCHAR(512) NOT NULL,
    is_authenticated BOOLEAN NOT NULL DEFAULT FALSE,
    first_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_analytics_presence_last_seen_at ON analytics_presence(last_seen_at DESC);
