CREATE TABLE IF NOT EXISTS resources (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS actions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS permission_scopes (
    permission_id UUID PRIMARY KEY REFERENCES permissions(id) ON DELETE CASCADE,
    resource_id UUID NOT NULL REFERENCES resources(id) ON DELETE RESTRICT,
    action_id UUID NOT NULL REFERENCES actions(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_permission_scopes_resource_id ON permission_scopes(resource_id);
CREATE INDEX IF NOT EXISTS idx_permission_scopes_action_id ON permission_scopes(action_id);

-- Resource catalog
INSERT INTO resources (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'RESEARCH_PAPERS', 'Research papers domain resource', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO resources (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'RECRUITMENT_POSTS', 'Recruitment posts domain resource', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO resources (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'MODERATION_POSTS', 'Moderation queue for recruitment posts', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO resources (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'MODERATION_PAPERS', 'Moderation queue for research papers', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO resources (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'RESEARCH_HERO_CONTENT', 'Hero content on research homepage', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO resources (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'ADMIN_DASHBOARD', 'Administrative dashboard access', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO resources (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'RBAC', 'Role-based access control administration', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Action catalog
INSERT INTO actions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'VIEW_APPROVED', 'Read approved/public content', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO actions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'CREATE', 'Create new content', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO actions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'EDIT_OWN', 'Edit own content', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO actions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'VIEW_QUEUE', 'Read moderation queue', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO actions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'APPROVE_REJECT', 'Approve or reject pending content', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO actions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'EDIT', 'Edit managed content', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO actions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'ACCESS', 'Access protected area', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO actions (id, name, description, created_at)
VALUES (uuid_generate_v4(), 'MANAGE', 'Manage catalog and assignments', CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Permission -> (resource, action)
INSERT INTO permission_scopes (permission_id, resource_id, action_id)
SELECT p.id, r.id, a.id
FROM permissions p
JOIN resources r ON r.name = 'RESEARCH_PAPERS'
JOIN actions a ON a.name = 'VIEW_APPROVED'
WHERE p.name = 'RESEARCH_VIEW'
ON CONFLICT (permission_id) DO UPDATE
SET resource_id = EXCLUDED.resource_id,
    action_id = EXCLUDED.action_id;

INSERT INTO permission_scopes (permission_id, resource_id, action_id)
SELECT p.id, r.id, a.id
FROM permissions p
JOIN resources r ON r.name = 'RESEARCH_PAPERS'
JOIN actions a ON a.name = 'CREATE'
WHERE p.name = 'RESEARCH_CREATE'
ON CONFLICT (permission_id) DO UPDATE
SET resource_id = EXCLUDED.resource_id,
    action_id = EXCLUDED.action_id;

INSERT INTO permission_scopes (permission_id, resource_id, action_id)
SELECT p.id, r.id, a.id
FROM permissions p
JOIN resources r ON r.name = 'RESEARCH_PAPERS'
JOIN actions a ON a.name = 'EDIT_OWN'
WHERE p.name = 'RESEARCH_EDIT_OWN'
ON CONFLICT (permission_id) DO UPDATE
SET resource_id = EXCLUDED.resource_id,
    action_id = EXCLUDED.action_id;

INSERT INTO permission_scopes (permission_id, resource_id, action_id)
SELECT p.id, r.id, a.id
FROM permissions p
JOIN resources r ON r.name = 'RECRUITMENT_POSTS'
JOIN actions a ON a.name = 'VIEW_APPROVED'
WHERE p.name = 'RECRUITMENT_VIEW'
ON CONFLICT (permission_id) DO UPDATE
SET resource_id = EXCLUDED.resource_id,
    action_id = EXCLUDED.action_id;

INSERT INTO permission_scopes (permission_id, resource_id, action_id)
SELECT p.id, r.id, a.id
FROM permissions p
JOIN resources r ON r.name = 'ADMIN_DASHBOARD'
JOIN actions a ON a.name = 'ACCESS'
WHERE p.name = 'ADMIN_DASHBOARD_VIEW'
ON CONFLICT (permission_id) DO UPDATE
SET resource_id = EXCLUDED.resource_id,
    action_id = EXCLUDED.action_id;

INSERT INTO permission_scopes (permission_id, resource_id, action_id)
SELECT p.id, r.id, a.id
FROM permissions p
JOIN resources r ON r.name = 'MODERATION_POSTS'
JOIN actions a ON a.name = 'VIEW_QUEUE'
WHERE p.name = 'MODERATION_POSTS_VIEW'
ON CONFLICT (permission_id) DO UPDATE
SET resource_id = EXCLUDED.resource_id,
    action_id = EXCLUDED.action_id;

INSERT INTO permission_scopes (permission_id, resource_id, action_id)
SELECT p.id, r.id, a.id
FROM permissions p
JOIN resources r ON r.name = 'MODERATION_POSTS'
JOIN actions a ON a.name = 'APPROVE_REJECT'
WHERE p.name = 'MODERATION_POSTS_ACTION'
ON CONFLICT (permission_id) DO UPDATE
SET resource_id = EXCLUDED.resource_id,
    action_id = EXCLUDED.action_id;

INSERT INTO permission_scopes (permission_id, resource_id, action_id)
SELECT p.id, r.id, a.id
FROM permissions p
JOIN resources r ON r.name = 'MODERATION_PAPERS'
JOIN actions a ON a.name = 'VIEW_QUEUE'
WHERE p.name = 'MODERATION_PAPERS_VIEW'
ON CONFLICT (permission_id) DO UPDATE
SET resource_id = EXCLUDED.resource_id,
    action_id = EXCLUDED.action_id;

INSERT INTO permission_scopes (permission_id, resource_id, action_id)
SELECT p.id, r.id, a.id
FROM permissions p
JOIN resources r ON r.name = 'MODERATION_PAPERS'
JOIN actions a ON a.name = 'APPROVE_REJECT'
WHERE p.name = 'MODERATION_PAPERS_ACTION'
ON CONFLICT (permission_id) DO UPDATE
SET resource_id = EXCLUDED.resource_id,
    action_id = EXCLUDED.action_id;

INSERT INTO permission_scopes (permission_id, resource_id, action_id)
SELECT p.id, r.id, a.id
FROM permissions p
JOIN resources r ON r.name = 'RESEARCH_HERO_CONTENT'
JOIN actions a ON a.name = 'EDIT'
WHERE p.name = 'RESEARCH_HERO_EDIT'
ON CONFLICT (permission_id) DO UPDATE
SET resource_id = EXCLUDED.resource_id,
    action_id = EXCLUDED.action_id;

INSERT INTO permission_scopes (permission_id, resource_id, action_id)
SELECT p.id, r.id, a.id
FROM permissions p
JOIN resources r ON r.name = 'RBAC'
JOIN actions a ON a.name = 'MANAGE'
WHERE p.name = 'RBAC_MANAGE'
ON CONFLICT (permission_id) DO UPDATE
SET resource_id = EXCLUDED.resource_id,
    action_id = EXCLUDED.action_id;
