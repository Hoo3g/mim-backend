-- ============================================================
-- V19: Recruitment categories taxonomy for recruitment filters
-- ============================================================

CREATE TABLE IF NOT EXISTS recruitment_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(120) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_recruitment_categories_name_lower
    ON recruitment_categories ((LOWER(name)));

CREATE INDEX IF NOT EXISTS idx_recruitment_categories_active_sort
    ON recruitment_categories(active, sort_order, name);

INSERT INTO recruitment_categories (id, name, sort_order, active, created_at, updated_at)
VALUES
    (uuid_generate_v4(), 'Backend', 10, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'Frontend', 20, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'Fullstack', 30, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'AI', 40, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'Mobile', 50, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'Game', 60, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'Data', 70, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'DevOps', 80, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'QA', 90, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'UI/UX', 100, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
