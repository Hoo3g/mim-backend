-- Ensure core roles exist
INSERT INTO roles (id, name, description, created_at)
SELECT uuid_generate_v4(), 'STUDENT', 'Student role', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'STUDENT');

INSERT INTO roles (id, name, description, created_at)
SELECT uuid_generate_v4(), 'LECTURER', 'Lecturer role', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'LECTURER');

INSERT INTO roles (id, name, description, created_at)
SELECT uuid_generate_v4(), 'COMPANY', 'Company role', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'COMPANY');

INSERT INTO roles (id, name, description, created_at)
SELECT uuid_generate_v4(), 'ADMIN', 'Admin role', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');

-- Permission catalog
INSERT INTO permissions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'RESEARCH_VIEW', 'View approved research papers', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'RESEARCH_CREATE', 'Create research papers', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'RESEARCH_EDIT_OWN', 'Edit own research papers', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'RECRUITMENT_VIEW', 'View approved recruitment posts', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'ADMIN_DASHBOARD_VIEW', 'Access admin dashboard', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'MODERATION_POSTS_VIEW', 'View post moderation queue', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'MODERATION_POSTS_ACTION', 'Approve/reject posts', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'MODERATION_PAPERS_VIEW', 'View paper moderation queue', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'MODERATION_PAPERS_ACTION', 'Approve/reject papers', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'RESEARCH_HERO_EDIT', 'Edit research hero content', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'RBAC_MANAGE', 'Manage RBAC assignments', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Role -> permission mappings
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('RESEARCH_VIEW', 'RESEARCH_CREATE', 'RESEARCH_EDIT_OWN', 'RECRUITMENT_VIEW')
WHERE r.name = 'STUDENT'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('RESEARCH_VIEW', 'RECRUITMENT_VIEW')
WHERE r.name = 'COMPANY'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('RESEARCH_VIEW', 'RESEARCH_CREATE', 'RESEARCH_EDIT_OWN', 'RECRUITMENT_VIEW')
WHERE r.name = 'LECTURER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'RESEARCH_VIEW',
    'RESEARCH_CREATE',
    'RESEARCH_EDIT_OWN',
    'RECRUITMENT_VIEW',
    'ADMIN_DASHBOARD_VIEW',
    'MODERATION_POSTS_VIEW',
    'MODERATION_POSTS_ACTION',
    'MODERATION_PAPERS_VIEW',
    'MODERATION_PAPERS_ACTION',
    'RESEARCH_HERO_EDIT',
    'RBAC_MANAGE'
)
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Per-user permission overrides (grant/deny)
CREATE TABLE IF NOT EXISTS user_permissions (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    effect VARCHAR(10) NOT NULL CHECK (effect IN ('GRANT', 'DENY')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_user_permissions_user_id ON user_permissions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_permissions_permission_id ON user_permissions(permission_id);
