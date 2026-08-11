-- V2__seed_users.sql
-- Creates the initial users and user_roles tables (integer PK — superseded by V5)
-- and seeds the three test users with their role assignments.
--
-- Passwords are BCrypt-encoded with cost factor 10.
-- Plain-text password for all seed users: Test1234!
-- Regenerate with: new BCryptPasswordEncoder().encode("Test1234!")

CREATE TABLE users (
    id         SERIAL       NOT NULL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    username   VARCHAR(100) NOT NULL UNIQUE,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);

CREATE TABLE user_roles (
    user_id INT NOT NULL REFERENCES users(id),
    role_id INT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

INSERT INTO users (name, username, email, password, created_at, updated_at)
VALUES
    ('Admin User',  'admin',    'admin@noteplus.nl',   '$2a$slYQmyNdgTY18LGvgxLJLuqSTTaVKFKzLHjXnAP5RQyb6MBtFm.Dm', NOW(), NOW()),
    ('Student One', 'student1', 'student@noteplus.nl', '$2a$slYQmyNdgTY18LGvgxLJLuqSTTaVKFKzLHjXnAP5RQyb6MBtFm.Dm', NOW(), NOW()),
    ('Coach One',   'coach1',   'coach@noteplus.nl',   '$2a$slYQmyNdgTY18LGvgxLJLuqSTTaVKFKzLHjXnAP5RQyb6MBtFm.Dm', NOW(), NOW());

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'student1' AND r.name = 'ROLE_STUDENT';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'coach1' AND r.name = 'ROLE_COACH';