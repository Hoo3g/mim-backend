CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE INDEX IF NOT EXISTS idx_posts_approval_created_at
    ON posts(approval_status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_posts_approval_post_type_created_at
    ON posts(approval_status, post_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_research_papers_approval_created_at
    ON research_papers(approval_status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_research_papers_approval_category_created_at
    ON research_papers(approval_status, category, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_research_papers_approval_research_area_created_at
    ON research_papers(approval_status, research_area, created_at DESC);
