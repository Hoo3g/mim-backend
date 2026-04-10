CREATE INDEX IF NOT EXISTS idx_analytics_page_views_visitor_route_path_occurred_at
    ON analytics_page_views(visitor_id, route_key, path, occurred_at DESC);
