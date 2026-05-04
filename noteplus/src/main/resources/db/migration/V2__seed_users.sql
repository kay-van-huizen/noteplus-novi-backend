-- Passwords are BCrypt-encoded with cost factor 10.
-- Plain-text password for all seed users: Test1234!
-- Regenerate with: new BCryptPasswordEncoder().encode("Test1234!")

INSERT INTO users (name, username, email, password, created_at, updated_at)
VALUES
    ('Admin User',  'admin',    'admin@noteplus.nl',   '$2a$10$slYQmyNdgTY18LGvgxLJLuqSTTaVKFKzLHjXnAP5RQyb6MBtFm.Dm', NOW(), NOW()),
    ('Student One', 'student1', 'student@noteplus.nl', '$2a$10$slYQmyNdgTY18LGvgxLJLuqSTTaVKFKzLHjXnAP5RQyb6MBtFm.Dm', NOW(), NOW()),
    ('Coach One',   'coach1',   'coach@noteplus.nl',   '$2a$10$slYQmyNdgTY18LGvgxLJLuqSTTaVKFKzLHjXnAP5RQyb6MBtFm.Dm', NOW(), NOW());

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'student1' AND r.name = 'ROLE_STUDENT';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'coach1' AND r.name = 'ROLE_COACH';
