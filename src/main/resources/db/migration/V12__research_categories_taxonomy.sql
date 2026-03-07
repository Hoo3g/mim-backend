-- ============================================================
-- V12: Dynamic research categories managed by admin
-- ============================================================

CREATE TABLE IF NOT EXISTS research_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(120) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_research_categories_name_lower
    ON research_categories ((LOWER(name)));

CREATE INDEX IF NOT EXISTS idx_research_categories_active_sort
    ON research_categories(active, sort_order, name);

-- Seed baseline taxonomy (idempotent)
INSERT INTO research_categories (id, name, sort_order, active, created_at, updated_at)
VALUES
    (uuid_generate_v4(), 'Trí tuệ nhân tạo', 10, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'An ninh mạng', 20, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'Khoa học dữ liệu', 30, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'Blockchain', 40, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'IoT', 50, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'Toán ứng dụng', 60, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'Kinh tế số', 70, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'Chưa phân loại', 999, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Backfill categories from existing papers so old values can still be selected
INSERT INTO research_categories (id, name, sort_order, active, created_at, updated_at)
SELECT uuid_generate_v4(), TRIM(rp.research_area), 500, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM research_papers rp
WHERE rp.research_area IS NOT NULL
  AND BTRIM(rp.research_area) <> ''
ON CONFLICT DO NOTHING;

-- Keep existing papers valid for strict category validation
UPDATE research_papers
SET research_area = 'Chưa phân loại'
WHERE research_area IS NULL OR BTRIM(research_area) = '';

-- New RBAC permission for category management
INSERT INTO permissions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'RESEARCH_CATEGORY_MANAGE', 'Manage research category taxonomy', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name = 'RESEARCH_CATEGORY_MANAGE'
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO resources (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'RESEARCH_CATEGORIES', 'Research category taxonomy catalog', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO permission_scopes (permission_id, resource_id, action_id)
SELECT p.id, r.id, a.id
FROM permissions p
JOIN resources r ON r.name = 'RESEARCH_CATEGORIES'
JOIN actions a ON a.name = 'MANAGE'
WHERE p.name = 'RESEARCH_CATEGORY_MANAGE'
ON CONFLICT (permission_id) DO UPDATE
SET resource_id = EXCLUDED.resource_id,
    action_id = EXCLUDED.action_id;
