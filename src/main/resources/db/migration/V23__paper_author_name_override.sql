ALTER TABLE paper_authors
ADD COLUMN IF NOT EXISTS author_name_override VARCHAR(255);
