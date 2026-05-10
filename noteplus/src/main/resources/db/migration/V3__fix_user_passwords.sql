-- Flyway placeholder replacement stripped '$10$' from the BCrypt hashes during V2.
-- Stored value was: $2a$slYQ... (invalid — missing cost factor)
-- Correct value is: $2a$10$slYQ... (cost=10, password=Test1234!)
--
-- String concatenation is used here so no single token contains $digit$ that
-- Flyway could misinterpret, even if placeholder-replacement is accidentally re-enabled.

UPDATE users
SET password = '$2a$' || '10$' || 'slYQmyNdgTY18LGvgxLJLuqSTTaVKFKzLHjXnAP5RQyb6MBtFm.Dm'
WHERE username IN ('admin', 'student1', 'coach1');
