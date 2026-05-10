-- V4__reset_user_passwords.sql
-- Fixes auth by generating BCrypt hashes inside PostgreSQL via pgcrypto.
-- pgcrypto's crypt() with 'bf' (Blowfish) produces $2a$ hashes that are
-- 100% compatible with Spring Security's BCryptPasswordEncoder.
--
-- Password for all three users: Test1234!

CREATE EXTENSION IF NOT EXISTS pgcrypto;

UPDATE users
SET password = crypt('Test1234!', gen_salt('bf', 10))
WHERE username IN ('admin', 'student1', 'coach1');
