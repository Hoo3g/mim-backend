-- ============================================================
-- MIM Project - Local development seed data
-- Idempotent seed for demo/testing on a fresh local database.
-- Requires pgcrypto extension enabled by docker/init.sql.
-- ============================================================

BEGIN;

-- Stable ids make the seed re-runnable and easy to inspect.
-- Password for all users below: Password123!

-- ------------------------------------------------------------
-- Users
-- ------------------------------------------------------------
INSERT INTO users (
    id,
    email,
    password,
    avatar_url,
    account_status,
    created_at,
    updated_at
)
VALUES
    (
        '11111111-1111-1111-1111-111111111111',
        'admin@mim.local',
        crypt('Password123!', gen_salt('bf')),
        'https://ui-avatars.com/api/?name=Admin&background=222&color=fff',
        'APPROVED',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '22222222-2222-2222-2222-222222222222',
        'company@mim.local',
        crypt('Password123!', gen_salt('bf')),
        'https://ui-avatars.com/api/?name=Data+Lab&background=0B7285&color=fff',
        'APPROVED',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '33333333-3333-3333-3333-333333333333',
        'lecturer@mim.local',
        crypt('Password123!', gen_salt('bf')),
        'https://ui-avatars.com/api/?name=Lecturer&background=364FC7&color=fff',
        'APPROVED',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '44444444-4444-4444-4444-444444444444',
        'student@mim.local',
        crypt('Password123!', gen_salt('bf')),
        'https://ui-avatars.com/api/?name=Student&background=2B8A3E&color=fff',
        'APPROVED',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT '11111111-1111-1111-1111-111111111111', r.id
FROM roles r
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT '22222222-2222-2222-2222-222222222222', r.id
FROM roles r
WHERE r.name = 'COMPANY'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT '33333333-3333-3333-3333-333333333333', r.id
FROM roles r
WHERE r.name = 'LECTURER'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT '44444444-4444-4444-4444-444444444444', r.id
FROM roles r
WHERE r.name = 'STUDENT'
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- Profiles
-- ------------------------------------------------------------
INSERT INTO companies (
    id,
    name,
    industry,
    website,
    location,
    description,
    logo_url,
    updated_at
)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    'MIM Data Lab',
    'Education Technology',
    'https://mim.local/company',
    'Ha Noi',
    'Company profile for local development and recruitment testing.',
    'https://ui-avatars.com/api/?name=MIM+Data+Lab&background=0B7285&color=fff',
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO lecturers (
    id,
    first_name,
    last_name,
    title,
    academic_rank,
    bio,
    research_interests,
    avatar_url,
    updated_at
)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    'Lan',
    'Nguyen',
    'Dr.',
    'Associate Professor',
    'Lecturer profile for local research portal testing.',
    ARRAY['Artificial Intelligence', 'Data Mining'],
    'https://ui-avatars.com/api/?name=Lan+Nguyen&background=364FC7&color=fff',
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO students (
    id,
    first_name,
    last_name,
    university,
    major,
    bio,
    cv_url,
    student_type,
    student_code,
    achievements,
    career_goal,
    desired_position,
    updated_at
)
VALUES (
    '44444444-4444-4444-4444-444444444444',
    'Minh',
    'Tran',
    'VNU University of Science',
    'Khoa hoc du lieu',
    'Student profile for local development and job application testing.',
    '/api/public/storage/profile-cvs/minh-tran-cv.pdf',
    'UNIVERSITY_STUDENT',
    'SV2026001',
    'Top 5 student research contest 2025',
    'Become an ML engineer focused on education.',
    'Machine Learning Intern',
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- News
-- ------------------------------------------------------------
INSERT INTO news (
    id,
    title,
    content,
    summary,
    author_id,
    status,
    image_url,
    pinned,
    created_at,
    updated_at
)
VALUES
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
        'MIM research portal demo is ready',
        'This seeded news entry exists so the frontend can render a non-empty news list during local development.',
        'Demo news item for local development.',
        '11111111-1111-1111-1111-111111111111',
        'PUBLISHED',
        'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1200&q=80',
        TRUE,
        CURRENT_TIMESTAMP - INTERVAL '2 days',
        CURRENT_TIMESTAMP - INTERVAL '2 days'
    ),
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2',
        'Student research and recruitment flow test data',
        'Use this entry to verify the homepage, news detail screen, and admin news listing.',
        'Second published news item for the seeded local database.',
        '11111111-1111-1111-1111-111111111111',
        'PUBLISHED',
        'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1200&q=80',
        FALSE,
        CURRENT_TIMESTAMP - INTERVAL '1 day',
        CURRENT_TIMESTAMP - INTERVAL '1 day'
    )
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- Research paper
-- ------------------------------------------------------------
INSERT INTO research_papers (
    id,
    title,
    abstract,
    pdf_url,
    publication_year,
    journal_conference,
    research_area,
    research_areas,
    category,
    view_count,
    download_count,
    citation_count,
    approval_status,
    moderation_comment,
    moderator_id,
    created_at,
    updated_at
)
VALUES (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
    'Applied AI for Student Research Discovery',
    'A seeded approved research paper used to verify the public research listing, detail page, and post-paper linking.',
    '/api/public/storage/research-pdfs/demo-ai-research.pdf',
    2025,
    'MIM Draft',
    'Blockchain',
    ARRAY['Blockchain'],
    'LECTURER',
    42,
    12,
    3,
    'APPROVED',
    'Seeded as approved for local demo purposes.',
    '11111111-1111-1111-1111-111111111111',
    CURRENT_TIMESTAMP - INTERVAL '5 days',
    CURRENT_TIMESTAMP - INTERVAL '5 days'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO paper_authors (
    id,
    paper_id,
    student_id,
    lecturer_id,
    is_main_author,
    author_order
)
VALUES (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
    NULL,
    '33333333-3333-3333-3333-333333333333',
    TRUE,
    1
)
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- Recruitment posts
-- ------------------------------------------------------------
INSERT INTO posts (
    id,
    author_id,
    title,
    description,
    requirements,
    benefits,
    achievements,
    post_type,
    job_type,
    student_cv_url,
    display_info,
    location,
    salary_range,
    status,
    approval_status,
    moderation_comment,
    contact_email,
    contact_phone,
    tags,
    view_count,
    moderator_id,
    created_at,
    updated_at
)
VALUES
    (
        'cccccccc-cccc-cccc-cccc-ccccccccccc1',
        '22222222-2222-2222-2222-222222222222',
        'Data Science Internship - Spring 2026',
        'Approved company post used by the frontend to render the public recruitment list.',
        'Python, SQL, statistics fundamentals',
        'Mentoring, monthly allowance, flexible hybrid work',
        NULL,
        'COMPANY_RECRUITING_INTERNSHIP',
        'INTERNSHIP',
        NULL,
        '{"companyName":"MIM Data Lab","deadline":"2026-04-30"}'::jsonb,
        'Ha Noi',
        '8,000,000 - 12,000,000 VND',
        'OPEN',
        'APPROVED',
        'Seeded as approved for local demo purposes.',
        'hr@mim.local',
        '0123456789',
        ARRAY['data-science', 'internship', 'python'],
        73,
        '11111111-1111-1111-1111-111111111111',
        CURRENT_TIMESTAMP - INTERVAL '3 days',
        CURRENT_TIMESTAMP - INTERVAL '3 days'
    ),
    (
        'cccccccc-cccc-cccc-cccc-ccccccccccc2',
        '44444444-4444-4444-4444-444444444444',
        'Student seeking ML internship',
        'Approved student post used to verify student-originated recruitment entries.',
        'Looking for mentoring in machine learning and backend APIs.',
        'Open to research assistant or internship opportunities.',
        'Built an NLP chatbot for coursework.',
        'STUDENT_SEEKING_INTERNSHIP',
        'INTERNSHIP',
        '/api/public/storage/profile-cvs/minh-tran-cv.pdf',
        '{"studentName":"Minh Tran","availability":"Part-time"}'::jsonb,
        'Ha Noi',
        'Negotiable',
        'OPEN',
        'APPROVED',
        'Seeded as approved for local demo purposes.',
        'student@mim.local',
        '0987654321',
        ARRAY['machine-learning', 'student', 'internship'],
        25,
        '11111111-1111-1111-1111-111111111111',
        CURRENT_TIMESTAMP - INTERVAL '2 days',
        CURRENT_TIMESTAMP - INTERVAL '2 days'
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO post_paper_links (post_id, paper_id)
VALUES (
    'cccccccc-cccc-cccc-cccc-ccccccccccc2',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1'
)
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- Application / bookmarks
-- ------------------------------------------------------------
INSERT INTO applications (
    id,
    post_id,
    applicant_id,
    status,
    message,
    cv_url,
    created_at
)
VALUES (
    'dddddddd-dddd-dddd-dddd-ddddddddddd1',
    'cccccccc-cccc-cccc-cccc-ccccccccccc1',
    '44444444-4444-4444-4444-444444444444',
    'PENDING',
    'I would like to apply for the seeded internship position.',
    '/api/public/storage/profile-cvs/minh-tran-cv.pdf',
    CURRENT_TIMESTAMP - INTERVAL '12 hours'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO saved_posts (user_id, post_id, created_at)
VALUES (
    '44444444-4444-4444-4444-444444444444',
    'cccccccc-cccc-cccc-cccc-ccccccccccc1',
    CURRENT_TIMESTAMP - INTERVAL '10 hours'
)
ON CONFLICT DO NOTHING;

INSERT INTO saved_research_papers (user_id, paper_id, created_at)
VALUES (
    '44444444-4444-4444-4444-444444444444',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
    CURRENT_TIMESTAMP - INTERVAL '8 hours'
)
ON CONFLICT DO NOTHING;

COMMIT;
