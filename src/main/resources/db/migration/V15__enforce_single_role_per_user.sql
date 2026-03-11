-- Enforce one role per user in RBAC assignments.
-- 1) Keep only one role for each user (prefer role matching existing profile table).
-- 2) Ensure users without role receive a default role.
-- 3) Add DB-level uniqueness to prevent future multi-role assignments.

WITH ranked_roles AS (
    SELECT
        ur.user_id,
        ur.role_id,
        ROW_NUMBER() OVER (
            PARTITION BY ur.user_id
            ORDER BY
                CASE
                    WHEN s.id IS NOT NULL AND UPPER(r.name) = 'STUDENT' THEN 1
                    WHEN c.id IS NOT NULL AND UPPER(r.name) = 'COMPANY' THEN 1
                    WHEN l.id IS NOT NULL AND UPPER(r.name) = 'LECTURER' THEN 1
                    WHEN UPPER(r.name) = 'ADMIN' THEN 2
                    WHEN UPPER(r.name) = 'STUDENT' THEN 3
                    WHEN UPPER(r.name) = 'COMPANY' THEN 4
                    WHEN UPPER(r.name) = 'LECTURER' THEN 5
                    ELSE 99
                END,
                UPPER(r.name),
                ur.role_id
        ) AS rn
    FROM user_roles ur
    JOIN roles r ON r.id = ur.role_id
    LEFT JOIN students s ON s.id = ur.user_id
    LEFT JOIN companies c ON c.id = ur.user_id
    LEFT JOIN lecturers l ON l.id = ur.user_id
)
DELETE FROM user_roles ur
USING ranked_roles rr
WHERE ur.user_id = rr.user_id
  AND ur.role_id = rr.role_id
  AND rr.rn > 1;

WITH role_ids AS (
    SELECT
        (
            SELECT id
            FROM roles
            WHERE UPPER(name) = 'STUDENT'
            ORDER BY id
            LIMIT 1
        ) AS student_role_id,
        (
            SELECT id
            FROM roles
            WHERE UPPER(name) = 'COMPANY'
            ORDER BY id
            LIMIT 1
        ) AS company_role_id,
        (
            SELECT id
            FROM roles
            WHERE UPPER(name) = 'LECTURER'
            ORDER BY id
            LIMIT 1
        ) AS lecturer_role_id
),
candidate_roles AS (
    SELECT
        u.id AS user_id,
        CASE
            WHEN s.id IS NOT NULL THEN ri.student_role_id
            WHEN c.id IS NOT NULL THEN ri.company_role_id
            WHEN l.id IS NOT NULL THEN ri.lecturer_role_id
            ELSE ri.student_role_id
        END AS role_id
    FROM users u
    CROSS JOIN role_ids ri
    LEFT JOIN students s ON s.id = u.id
    LEFT JOIN companies c ON c.id = u.id
    LEFT JOIN lecturers l ON l.id = u.id
    WHERE NOT EXISTS (
        SELECT 1
        FROM user_roles ur
        WHERE ur.user_id = u.id
    )
)
INSERT INTO user_roles (user_id, role_id)
SELECT user_id, role_id
FROM candidate_roles
WHERE role_id IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_user_roles_user_id'
          AND conrelid = 'user_roles'::regclass
    ) THEN
        ALTER TABLE user_roles
            ADD CONSTRAINT uq_user_roles_user_id UNIQUE (user_id);
    END IF;
END
$$;
