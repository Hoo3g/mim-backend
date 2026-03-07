-- ============================================================
-- V13: Backfill shared specializations from existing student majors
-- ============================================================

INSERT INTO research_categories (id, name, sort_order, active, created_at, updated_at)
SELECT uuid_generate_v4(), TRIM(s.major), 450, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM students s
WHERE s.major IS NOT NULL
  AND BTRIM(s.major) <> ''
ON CONFLICT DO NOTHING;
