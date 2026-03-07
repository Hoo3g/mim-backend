-- ============================================================
-- V11: Profile dashboard, research bookmarks, and apply indexes
-- ============================================================

-- Student extended profile fields
ALTER TABLE students ADD COLUMN IF NOT EXISTS achievements TEXT;
ALTER TABLE students ADD COLUMN IF NOT EXISTS career_goal TEXT;
ALTER TABLE students ADD COLUMN IF NOT EXISTS desired_position VARCHAR(255);

-- Saved research papers for profile dashboard
CREATE TABLE IF NOT EXISTS saved_research_papers (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    paper_id UUID NOT NULL REFERENCES research_papers(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, paper_id)
);

CREATE INDEX IF NOT EXISTS idx_saved_research_papers_user_id
    ON saved_research_papers(user_id);

CREATE INDEX IF NOT EXISTS idx_saved_research_papers_paper_id
    ON saved_research_papers(paper_id);

-- Pending-application dashboard query performance
CREATE INDEX IF NOT EXISTS idx_applications_applicant_status_created
    ON applications(applicant_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_applications_post_status_created
    ON applications(post_id, status, created_at DESC);
