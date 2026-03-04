-- ============================================================
-- V6: Supplement Missing Features
-- Bổ sung các trường và bảng còn thiếu so với thiết kế frontend
-- ============================================================

-- -------------------------------------------------------
-- 1. students: Thêm mã sinh viên (dùng để đăng ký tài khoản)
-- -------------------------------------------------------
ALTER TABLE students ADD COLUMN student_code VARCHAR(20) UNIQUE;

-- -------------------------------------------------------
-- 2. companies: Thêm logo URL (thay thế generic avatar)
-- -------------------------------------------------------
ALTER TABLE companies ADD COLUMN logo_url VARCHAR(255);

-- -------------------------------------------------------
-- 3. lecturers: Thêm avatar URL
-- -------------------------------------------------------
ALTER TABLE lecturers ADD COLUMN avatar_url VARCHAR(255);

-- -------------------------------------------------------
-- 4. research_papers: Đổi sang mảng thẻ lĩnh vực
--    (hiện có research_area VARCHAR đơn lẻ → thêm mảng
--     để hỗ trợ nhiều thẻ như "Cơ học", "An ninh mạng")
-- -------------------------------------------------------
ALTER TABLE research_papers ADD COLUMN research_areas TEXT[];

-- Migrate dữ liệu cũ sang mảng nếu có
UPDATE research_papers
SET research_areas = ARRAY[research_area]
WHERE research_area IS NOT NULL;

-- Giữ lại research_area cũ để backward-compatible,
-- backend sẽ ưu tiên dùng research_areas khi có.

-- -------------------------------------------------------
-- 5. paper_authors: Thêm constraint đảm bảo mỗi hàng
--    chỉ có student_id HOẶC lecturer_id, không phải cả hai
-- -------------------------------------------------------
ALTER TABLE paper_authors ADD CONSTRAINT chk_paper_author_one_type
    CHECK (
        (student_id IS NOT NULL AND lecturer_id IS NULL) OR
        (student_id IS NULL AND lecturer_id IS NOT NULL)
    );

-- -------------------------------------------------------
-- 6. post_paper_links: Bảng liên kết Posts ↔ Research Papers
--    Cho phép sinh viên gắn bài báo vào bài đăng tuyển dụng
-- -------------------------------------------------------
CREATE TABLE post_paper_links (
    post_id   UUID REFERENCES posts(id) ON DELETE CASCADE,
    paper_id  UUID REFERENCES research_papers(id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, paper_id)
);

CREATE INDEX idx_post_paper_links_post_id  ON post_paper_links(post_id);
CREATE INDEX idx_post_paper_links_paper_id ON post_paper_links(paper_id);

-- -------------------------------------------------------
-- 7. news: Bổ sung các trường quản trị Admin
--    (quản lý publish/draft, ảnh thumbnail, ghim tin)
-- -------------------------------------------------------
ALTER TABLE news ADD COLUMN status   VARCHAR(20)  DEFAULT 'DRAFT';   -- 'DRAFT' | 'PUBLISHED'
ALTER TABLE news ADD COLUMN image_url VARCHAR(255);                   -- Ảnh thumbnail
ALTER TABLE news ADD COLUMN pinned    BOOLEAN      DEFAULT FALSE;     -- Ghim tin lên đầu

-- Index hỗ trợ query list tin tức theo trạng thái và ghim
CREATE INDEX idx_news_status ON news(status);
CREATE INDEX idx_news_pinned ON news(pinned);

-- -------------------------------------------------------
-- 8. posts: Thêm lượt xem cho bài đăng tuyển dụng
-- -------------------------------------------------------
ALTER TABLE posts ADD COLUMN view_count INTEGER DEFAULT 0;

CREATE INDEX idx_posts_view_count ON posts(view_count);
