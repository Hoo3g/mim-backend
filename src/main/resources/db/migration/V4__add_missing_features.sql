-- Create News table for department bulletins
CREATE TABLE news (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(512) NOT NULL,
    content TEXT NOT NULL,
    summary TEXT, -- Optional short summary for list views
    author_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create Saved Posts table for bookmarks feature
CREATE TABLE saved_posts (
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    post_id UUID REFERENCES posts(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, post_id)
);

-- Add metrics to Research Papers
ALTER TABLE research_papers ADD COLUMN view_count INTEGER DEFAULT 0;
ALTER TABLE research_papers ADD COLUMN download_count INTEGER DEFAULT 0;
ALTER TABLE research_papers ADD COLUMN citation_count INTEGER DEFAULT 0;

-- Indices for new tables
CREATE INDEX idx_news_created_at ON news(created_at);
CREATE INDEX idx_saved_posts_user_id ON saved_posts(user_id);
