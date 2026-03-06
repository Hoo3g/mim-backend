-- ============================================================
-- V7: Auth Identifier and Student Code Indexes
-- ============================================================

-- Normalize existing student codes for case-insensitive uniqueness
UPDATE students
SET student_code = upper(trim(student_code))
WHERE student_code IS NOT NULL;

-- Ensure unique student code regardless of letter case
CREATE UNIQUE INDEX IF NOT EXISTS uq_students_student_code_upper
    ON students (upper(student_code))
    WHERE student_code IS NOT NULL;

-- Improve active refresh-token lookups by user
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_revoked_expiry
    ON refresh_tokens (user_id, revoked, expiry_date DESC);
