CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE OR REPLACE FUNCTION immutable_unaccent(text)
RETURNS text
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
AS $$
    SELECT unaccent('unaccent', $1)
$$;

CREATE INDEX IF NOT EXISTS idx_research_papers_title_search_trgm
    ON research_papers
    USING GIN (
        regexp_replace(
            immutable_unaccent(lower(COALESCE(title, ''))),
            '\s+',
            ' ',
            'g'
        ) gin_trgm_ops
    );
