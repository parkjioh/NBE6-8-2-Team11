-- PetMatching Database Initialization Script for PostgreSQL

-- Create database (optional, 대부분 Render 등에서는 이미 DB 생성)
-- CREATE DATABASE petmatching;

-- 연결할 DB 지정 (psql 실행 시 -d petmatching 사용)
-- \c petmatching

-- Create user if not exists and grant privileges
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles WHERE rolname = 'petmatching_user'
   ) THEN
CREATE ROLE petmatching_user LOGIN PASSWORD 'secure_user_password_change_me';
END IF;
END
$do$;

GRANT ALL PRIVILEGES ON DATABASE petmatching TO petmatching_user;

-- Create health_check table for testing
CREATE TABLE IF NOT EXISTS health_check (
                                            id SERIAL PRIMARY KEY,
                                            status VARCHAR(10) DEFAULT 'OK',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

INSERT INTO health_check (status) VALUES ('OK');

-- Create users table
CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- Insert admin user
INSERT INTO users (email, password, role)
VALUES ('admin@example.com', '1234', 'ADMIN')
    ON CONFLICT DO NOTHING;

-- Success message
SELECT 'PetMatching PostgreSQL database initialized successfully!' AS message;
