-- Add status to users for registration management
ALTER TABLE users ADD COLUMN account_status VARCHAR(20) DEFAULT 'PENDING';
-- Note: Logic should set 'APPROVED' immediately for Students registering with valid ID,
-- but stay 'PENDING' for others or require Admin manual creation.

-- Add moderation fields to posts
ALTER TABLE posts ADD COLUMN approval_status VARCHAR(20) DEFAULT 'PENDING';
ALTER TABLE posts ADD COLUMN moderator_id UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE posts ADD COLUMN moderation_comment TEXT;

-- Add moderation fields to research_papers
ALTER TABLE research_papers ADD COLUMN approval_status VARCHAR(20) DEFAULT 'PENDING';
ALTER TABLE research_papers ADD COLUMN moderator_id UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE research_papers ADD COLUMN moderation_comment TEXT;

-- Create Moderation Logs for audit trail
CREATE TABLE moderation_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    moderator_id UUID REFERENCES users(id) ON DELETE SET NULL,
    target_type VARCHAR(50) NOT NULL, -- e.g., 'POST', 'PAPER', 'USER'
    target_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL, -- 'APPROVE', 'REJECT'
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for status filtering
CREATE INDEX idx_posts_approval_status ON posts(approval_status);
CREATE INDEX idx_papers_approval_status ON research_papers(approval_status);
CREATE INDEX idx_users_account_status ON users(account_status);
