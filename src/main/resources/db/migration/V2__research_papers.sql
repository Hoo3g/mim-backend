-- Add student_type to students table
ALTER TABLE students ADD COLUMN student_type VARCHAR(50); -- e.g., 'PUPIL', 'UNIVERSITY_STUDENT'

-- Research Papers Table
CREATE TABLE research_papers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(512) NOT NULL,
    abstract TEXT, -- Editable via web (Plain text or simple HTML)
    pdf_url VARCHAR(255), -- Link to PDF for embedded viewing
    publication_year INTEGER,
    journal_conference VARCHAR(255),
    research_area VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Many-to-Many relationship between Papers and Students (Authors)
CREATE TABLE paper_authors (
    paper_id UUID REFERENCES research_papers(id) ON DELETE CASCADE,
    student_id UUID REFERENCES students(id) ON DELETE CASCADE,
    is_main_author BOOLEAN DEFAULT FALSE,
    author_order INTEGER DEFAULT 0,
    PRIMARY KEY (paper_id, student_id)
);

-- Indices for performance
CREATE INDEX idx_research_papers_title ON research_papers(title);
CREATE INDEX idx_paper_authors_paper_id ON paper_authors(paper_id);
CREATE INDEX idx_paper_authors_student_id ON paper_authors(student_id);
