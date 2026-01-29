-- Add avatar_url to users for profile identification
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(255);

-- Refine posts table for recruitment requirements
ALTER TABLE posts ADD COLUMN achievements TEXT;
ALTER TABLE posts ADD COLUMN contact_email VARCHAR(255);
ALTER TABLE posts ADD COLUMN contact_phone VARCHAR(50);
ALTER TABLE posts ADD COLUMN tags TEXT[]; -- Postgres array for tags

-- Create Lecturer table for faculty profiles
CREATE TABLE lecturers (
    id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    title VARCHAR(100), -- e.g., 'PGS.TS', 'GS.TS', 'TS'
    academic_rank VARCHAR(100),
    bio TEXT,
    research_interests TEXT[],
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Refine research_papers for broader categories
ALTER TABLE research_papers ADD COLUMN category VARCHAR(20) DEFAULT 'STUDENT'; -- 'STUDENT' or 'LECTURER'

-- Update paper_authors to support both students and lecturers
-- We drop the composite PK and add more robust references
ALTER TABLE paper_authors DROP CONSTRAINT paper_authors_pkey;
ALTER TABLE paper_authors ADD COLUMN lecturer_id UUID REFERENCES lecturers(id) ON DELETE CASCADE;
ALTER TABLE paper_authors ALTER COLUMN student_id DROP NOT NULL;

-- Re-establish primary key logic or use a serial ID for flexibility (optional, keeping primary key on pair for now)
-- ALTER TABLE paper_authors ADD PRIMARY KEY (paper_id, student_id, lecturer_id); 
-- Note: A many-to-many usually has one or the other. We'll handle constraints in code or add a unique ID.
ALTER TABLE paper_authors ADD COLUMN id UUID PRIMARY KEY DEFAULT uuid_generate_v4();

-- Indices for refined fields
CREATE INDEX idx_posts_tags ON posts USING GIN (tags);
CREATE INDEX idx_research_papers_category ON research_papers(category);
