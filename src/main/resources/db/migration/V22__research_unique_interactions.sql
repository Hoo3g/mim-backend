CREATE TABLE IF NOT EXISTS research_paper_unique_views (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    paper_id UUID NOT NULL REFERENCES research_papers(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, paper_id)
);

CREATE INDEX IF NOT EXISTS idx_research_paper_unique_views_paper_id
    ON research_paper_unique_views(paper_id);

CREATE TABLE IF NOT EXISTS research_paper_unique_downloads (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    paper_id UUID NOT NULL REFERENCES research_papers(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, paper_id)
);

CREATE INDEX IF NOT EXISTS idx_research_paper_unique_downloads_paper_id
    ON research_paper_unique_downloads(paper_id);

CREATE TABLE IF NOT EXISTS research_paper_unique_bookmarks (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    paper_id UUID NOT NULL REFERENCES research_papers(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, paper_id)
);

CREATE INDEX IF NOT EXISTS idx_research_paper_unique_bookmarks_paper_id
    ON research_paper_unique_bookmarks(paper_id);

INSERT INTO research_paper_unique_bookmarks (user_id, paper_id, created_at)
SELECT srp.user_id, srp.paper_id, srp.created_at
FROM saved_research_papers srp
ON CONFLICT (user_id, paper_id) DO NOTHING;
