-- ============================================================
-- V14: Separate specializations taxonomy from research categories
-- ============================================================

CREATE TABLE IF NOT EXISTS specializations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(120) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_specializations_name_lower
    ON specializations ((LOWER(name)));

CREATE INDEX IF NOT EXISTS idx_specializations_active_sort
    ON specializations(active, sort_order, name);

-- Baseline majors/specializations
INSERT INTO specializations (id, name, sort_order, active, created_at, updated_at)
VALUES
    (uuid_generate_v4(), 'Khoa học dữ liệu', 10, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'Khoa học máy tính', 20, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'Toán kinh tế', 30, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'Hệ thống thông tin', 40, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'An toàn thông tin', 50, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Backfill from student majors already in production data
INSERT INTO specializations (id, name, sort_order, active, created_at, updated_at)
SELECT uuid_generate_v4(), TRIM(s.major), 200, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM students s
WHERE s.major IS NOT NULL
  AND BTRIM(s.major) <> ''
ON CONFLICT DO NOTHING;
