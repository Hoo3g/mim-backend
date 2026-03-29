ALTER TABLE research_papers
    ADD COLUMN IF NOT EXISTS paper_type VARCHAR(40);

UPDATE research_papers
SET paper_type = 'SCIENTIFIC_RESEARCH'
WHERE paper_type IS NULL OR BTRIM(paper_type) = '';

ALTER TABLE research_papers
    ALTER COLUMN paper_type SET DEFAULT 'SCIENTIFIC_RESEARCH';

CREATE INDEX IF NOT EXISTS idx_research_papers_paper_type
    ON research_papers(paper_type);
