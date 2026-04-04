ALTER TABLE news
    ADD COLUMN content_type VARCHAR(64) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN import_source_url TEXT,
    ADD COLUMN schedule_entries_json TEXT,
    ADD COLUMN imported_at TIMESTAMP WITH TIME ZONE;
