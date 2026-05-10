# Auth Fix — NotePlus

## Root cause

`POST /api/auth/login` returns 403 "Invalid credentials" because Spring's
`BCryptPasswordEncoder` cannot match the stored password hash against `Test1234!`.
The V3 migration either did not run, or ran but stored a hash that doesn't match.

The Java code (AuthServiceImpl, SecurityConfig, CustomUserDetailsService) is all correct.
The problem is purely in the database.

---

## Fix — one file, drop and restart

### Step 1 — Copy migration file

Copy `V4__reset_user_passwords.sql` into:
```
src/main/resources/db/migration/V4__reset_user_passwords.sql
```

Content:
```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

UPDATE users
SET password = crypt('Test1234!', gen_salt('bf', 10))
WHERE username IN ('admin', 'student1', 'coach1');
```

### Step 2 — Restart the app

```bash
./mvnw spring-boot:run
```

Flyway will detect V4 as a new migration and run it automatically on startup.
You will see in the logs:
```
Flyway: Migrating schema "public" to version 4 - reset user passwords
```

### Step 3 — Verify in DBeaver (optional but recommended)

Open DBeaver → your Noteplus DB → run:
```sql
-- Check Flyway ran V4 successfully
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;

-- Check the hash looks like a real BCrypt hash (starts with $2a$10$)
SELECT username, LEFT(password, 20) AS hash_preview FROM users;
```

V4 `success` column should be `true`.
Hash preview should start with `$2a$10$`.

---

## Smoke test credentials (after restart)

| Username | Password  | Role         |
|----------|-----------|--------------|
| admin    | Test1234! | ROLE_ADMIN   |
| student1 | Test1234! | ROLE_STUDENT |
| coach1   | Test1234! | ROLE_COACH   |

---

## Swagger auth flow (step by step)

1. Open `http://localhost:8080/swagger-ui/index.html`
2. Find **POST /api/auth/login** → click **Try it out**
3. Paste body:
```json
{ "username": "admin", "password": "Test1234!" }
```
4. Execute → copy the `token` value from the response (just the string, no quotes)
5. Click the **Authorize** button (🔒) at the top of Swagger UI
6. Paste the token → click **Authorize** → click **Close**
7. All secured endpoints now work — padlock icons turn closed

---

## If V4 still fails

Check if any previous migration is marked as failed in Flyway:
```sql
SELECT version, description, success FROM flyway_schema_history;
```

If a row shows `success = false`, Flyway will refuse to run new migrations.
Fix:
```sql
DELETE FROM flyway_schema_history WHERE success = false;
```
Then restart.

---

## Why pgcrypto and not a hardcoded hash?

The V3 migration used a hardcoded BCrypt hash string. BCrypt hashes are
non-deterministic — even if the algorithm and cost are correct, you can't
verify a hash is valid for a given password without running it through BCrypt.

`pgcrypto`'s `crypt('Test1234!', gen_salt('bf', 10))` runs BCrypt inside
PostgreSQL at migration time and stores the result. Spring's
`BCryptPasswordEncoder.matches()` uses the same algorithm and will verify it correctly.
