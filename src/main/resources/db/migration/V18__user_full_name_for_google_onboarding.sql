ALTER TABLE users ADD COLUMN full_name VARCHAR(255);

UPDATE users u
SET full_name = COALESCE(
    NULLIF(c.name, ''),
    NULLIF(TRIM(COALESCE(s.first_name, '') || ' ' || COALESCE(s.last_name, '')), ''),
    NULLIF(TRIM(COALESCE(l.first_name, '') || ' ' || COALESCE(l.last_name, '')), ''),
    NULL
)
FROM users base
LEFT JOIN companies c ON c.id = base.id
LEFT JOIN students s ON s.id = base.id
LEFT JOIN lecturers l ON l.id = base.id
WHERE u.id = base.id
  AND (u.full_name IS NULL OR BTRIM(u.full_name) = '');
