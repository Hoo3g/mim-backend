CREATE TABLE research_hero_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    page_key VARCHAR(100) NOT NULL UNIQUE,
    title_prefix VARCHAR(255) NOT NULL,
    title_highlight VARCHAR(255) NOT NULL,
    subtitle TEXT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO research_hero_settings (
    page_key,
    title_prefix,
    title_highlight,
    subtitle,
    image_url
)
VALUES (
    'RESEARCH_HOME',
    'Nghiên cứu',
    'Đổi mới & Sáng tạo',
    'Nơi hội tụ những công trình nghiên cứu khoa học tiên phong của Khoa Toán - Cơ - Tin học.',
    'assets/faculty_building.png'
);
