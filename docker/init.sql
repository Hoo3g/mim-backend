-- ============================================================
-- MIM Project — PostgreSQL Initialization Script
-- Chạy tự động khi container khởi động lần đầu.
-- Flyway sẽ xử lý migrations (V1__...V6__...) sau khi backend start.
-- ============================================================

-- Extensions cần thiết
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pgcrypto";    -- crypt(), gen_salt()

-- Database test riêng (dùng cho CI/unit test, không ảnh hưởng main DB)
SELECT 'CREATE DATABASE mim_db_test OWNER ' || current_user
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'mim_db_test')\gexec

-- Grant đầy đủ quyền trên main DB
GRANT ALL PRIVILEGES ON DATABASE mim_db TO mim_user;
