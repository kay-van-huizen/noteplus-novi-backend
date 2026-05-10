-- V5__switch_pks_to_uuid.sql
-- Drops all tables and recreates them with UUID primary keys.
-- Existing data is discarded; seed data is re-inserted.
-- gen_random_uuid() requires pgcrypto (enabled in V4).

-- ─── 1. Drop junction tables first (no dependants) ───────────────────────────
DROP TABLE IF EXISTS learning_path_notes CASCADE;
DROP TABLE IF EXISTS note_references       CASCADE;
DROP TABLE IF EXISTS user_roles            CASCADE;

-- ─── 2. Drop leaf/child tables ───────────────────────────────────────────────
DROP TABLE IF EXISTS password_reset_tokens CASCADE;
DROP TABLE IF EXISTS file_attachments      CASCADE;
DROP TABLE IF EXISTS reference_items       CASCADE;
DROP TABLE IF EXISTS notes                 CASCADE;
DROP TABLE IF EXISTS learning_paths        CASCADE;
DROP TABLE IF EXISTS categories            CASCADE;
DROP TABLE IF EXISTS users                 CASCADE;
DROP TABLE IF EXISTS roles                 CASCADE;

-- ─── 3. Recreate roles ───────────────────────────────────────────────────────
CREATE TABLE roles (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name       VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL
);

-- ─── 4. Recreate users ───────────────────────────────────────────────────────
CREATE TABLE users (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    username     VARCHAR(100) NOT NULL UNIQUE,
    email        VARCHAR(150) NOT NULL UNIQUE,
    phone_number VARCHAR(30),
    password     VARCHAR(255) NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);

-- ─── 5. Recreate user_roles junction ─────────────────────────────────────────
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id),
    role_id UUID NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

-- ─── 6. Recreate categories (self-referential) ───────────────────────────────
CREATE TABLE categories (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    title       VARCHAR(100) NOT NULL,
    description TEXT,
    color       VARCHAR(30)  NOT NULL DEFAULT 'DEFAULT',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    parent_id   UUID REFERENCES categories(id),
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

-- ─── 7. Recreate notes ───────────────────────────────────────────────────────
CREATE TABLE notes (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    title       VARCHAR(150) NOT NULL,
    content     TEXT,
    user_id     UUID         NOT NULL REFERENCES users(id),
    category_id UUID         REFERENCES categories(id),
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

-- ─── 8. Recreate learning_paths ──────────────────────────────────────────────
CREATE TABLE learning_paths (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    title       VARCHAR(150) NOT NULL,
    description TEXT,
    student_id  UUID         NOT NULL REFERENCES users(id),
    coach_id    UUID         NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

-- ─── 9. Recreate file_attachments ────────────────────────────────────────────
CREATE TABLE file_attachments (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    file_name    VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    size         BIGINT       NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);

-- ─── 10. Recreate reference_items ────────────────────────────────────────────
CREATE TABLE reference_items (
    id                 UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    title              VARCHAR(150) NOT NULL,
    description        TEXT,
    url                VARCHAR(500),
    file_attachment_id UUID         UNIQUE REFERENCES file_attachments(id),
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL
);

-- ─── 11. Recreate note_references junction ───────────────────────────────────
CREATE TABLE note_references (
    note_id      UUID NOT NULL REFERENCES notes(id),
    reference_id UUID NOT NULL REFERENCES reference_items(id),
    PRIMARY KEY (note_id, reference_id)
);

-- ─── 12. Recreate learning_path_notes junction ───────────────────────────────
CREATE TABLE learning_path_notes (
    learning_path_id UUID NOT NULL REFERENCES learning_paths(id),
    note_id          UUID NOT NULL REFERENCES notes(id),
    PRIMARY KEY (learning_path_id, note_id)
);

-- ─── 13. Recreate password_reset_tokens ──────────────────────────────────────
CREATE TABLE password_reset_tokens (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    user_id    UUID        NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP   NOT NULL,
    used       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL
);

-- ─── 14. Re-seed roles ───────────────────────────────────────────────────────
INSERT INTO roles (name, created_at, updated_at) VALUES
    ('ROLE_ADMIN',   NOW(), NOW()),
    ('ROLE_STUDENT', NOW(), NOW()),
    ('ROLE_COACH',   NOW(), NOW());

-- ─── 15. Re-seed users (BCrypt hash for "Test1234!" generated via pgcrypto) ──
INSERT INTO users (name, username, email, password, created_at, updated_at) VALUES
    ('Admin User',  'admin',    'admin@noteplus.nl',   crypt('Test1234!', gen_salt('bf', 10)), NOW(), NOW()),
    ('Student One', 'student1', 'student@noteplus.nl', crypt('Test1234!', gen_salt('bf', 10)), NOW(), NOW()),
    ('Coach One',   'coach1',   'coach@noteplus.nl',   crypt('Test1234!', gen_salt('bf', 10)), NOW(), NOW());

-- ─── 16. Re-seed user_roles ──────────────────────────────────────────────────
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'student1' AND r.name = 'ROLE_STUDENT';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'coach1' AND r.name = 'ROLE_COACH';