# NotePlus — Context Gather for Claude

Run this top to bottom. For each file: read it and paste the **full content** into the reply.
The goal is to give Claude a complete picture of the current codebase so it can fix auth, generate seed data, and finalize the project.

---

## 1 — Project metadata

Read and paste full content of:
- `pom.xml`
- `src/main/resources/application.properties`

---

## 2 — Security layer (auth is broken — most critical)

Read and paste full content of:
- `src/main/java/org/noteplus/noteplus/security/JwtService.java`
- `src/main/java/org/noteplus/noteplus/security/JwtAuthenticationFilter.java`
- `src/main/java/org/noteplus/noteplus/security/JwtProperties.java`
- `src/main/java/org/noteplus/noteplus/security/SecurityConfig.java`
- `src/main/java/org/noteplus/noteplus/security/CustomUserDetailsService.java`

---

## 3 — Entities (need exact field names + id type for seed SQL)

Read and paste full content of:
- `src/main/java/org/noteplus/noteplus/entity/BaseEntity.java`
- `src/main/java/org/noteplus/noteplus/entity/User.java`
- `src/main/java/org/noteplus/noteplus/entity/Role.java`
- `src/main/java/org/noteplus/noteplus/entity/Note.java`
- `src/main/java/org/noteplus/noteplus/entity/Category.java`

---

## 4 — Repositories

Read and paste full content of:
- `src/main/java/org/noteplus/noteplus/repository/UserRepository.java`
- `src/main/java/org/noteplus/noteplus/repository/RoleRepository.java`

---

## 5 — Auth service + controller

Read and paste full content of:
- `src/main/java/org/noteplus/noteplus/service/impl/AuthServiceImpl.java`
- `src/main/java/org/noteplus/noteplus/controller/AuthController.java`

---

## 6 — Exception layer

Read and paste full content of:
- `src/main/java/org/noteplus/noteplus/exception/GlobalExceptionHandler.java`
- `src/main/java/org/noteplus/noteplus/exception/ForbiddenException.java`
- `src/main/java/org/noteplus/noteplus/exception/ResourceNotFoundException.java`
- `src/main/java/org/noteplus/noteplus/exception/DuplicateResourceException.java`

---

## 7 — Flyway migrations (if they exist)

Check if `src/main/resources/db/migration/` exists. If yes, paste all .sql files in it.
If the directory does not exist, write: `NO FLYWAY MIGRATIONS FOUND`

---

## 8 — Current state summary (run these in terminal, paste output)

```bash
# Schema: what tables exist in the DB right now
SELECT * FROM public.users
ORDER BY id ASC 
1	"2026-05-04 14:12:41.933514"	"2026-05-04 14:12:41.933514"	"admin@noteplus.nl"	"Admin User"	"$2a$10$slYQmyNdgTY18LGvgxLJLuqSTTaVKFKzLHjXnAP5RQyb6MBtFm.Dm"		"admin"
2	"2026-05-04 14:12:41.933514"	"2026-05-04 14:12:41.933514"	"student@noteplus.nl"	"Student One"	"$2a$10$slYQmyNdgTY18LGvgxLJLuqSTTaVKFKzLHjXnAP5RQyb6MBtFm.Dm"		"student1"
3	"2026-05-04 14:12:41.933514"	"2026-05-04 14:12:41.933514"	"coach@noteplus.nl"	"Coach One"	"$2a$10$slYQmyNdgTY18LGvgxLJLuqSTTaVKFKzLHjXnAP5RQyb6MBtFm.Dm"		"coach1"
4	"2026-05-04 14:32:36.421509"	"2026-05-04 14:32:36.421509"	"test@noteplus.nl"	"testuser"	"$2a$10$SpDHIA91jQ5qctUZo17IIevXyw9Yb5U.iho4MU1joxAEppVfs4/8e"		"testuser"
5	"2026-05-10 07:23:26.853806"	"2026-05-10 07:23:26.853806"	"user@example.com"	"string"	"$2a$10$gUIsH9R/n8fR5O2PDHbcKu4WuDy1FVWvn8nlDJMfK5c2K/j5UFXq2"		"string"
6	"2026-05-10 07:23:50.662188"	"2026-05-10 07:23:50.662188"	"admin@example.com"	"user"	"$2a$10$NBIjy/Pi7Y6LQA0VMbbAs.IVl3/jHJKMSsmMsPzZyXXr53yvsDJqq"		"user"
7	"2026-05-10 07:25:34.342864"	"2026-05-10 07:25:34.342864"	"kay@example.com"	"kay"	"$2a$10$wZEjBqwIO2HiYlcmWAtCx.E2xYVd8MtRPzYV3lZPMwfjkVqzsQHM2"		"kay"

# Roles in DB
SELECT * FROM public.roles
ORDER BY id ASC 
1	"2026-05-04 14:12:41.921861"	"2026-05-04 14:12:41.921861"	"ROLE_ADMIN"
2	"2026-05-04 14:12:41.921861"	"2026-05-04 14:12:41.921861"	"ROLE_STUDENT"
3	"2026-05-04 14:12:41.921861"	"2026-05-04 14:12:41.921861"	"ROLE_COACH"

# User-role assignments
SELECT * FROM public.user_roles
ORDER BY user_id ASC, role_id ASC 
1	1
2	2
3	3
4	2
5	2
6	2
7	2
```

---

## 9 — Known issues to fix (for Claude's reference)

Once you paste everything above back to Claude, ask it to fix these in order:

1. **Auth broken** — login returns 403 "Invalid credentials" for seeded users; tokens return "Invalid or expired token" in Swagger. Root cause unknown until security files are read.

2. **Seeded data** — i dont know the password of the generated users, so maybe we need to add the test-data again. Need Flyway migration with BCrypt-hashed passwords for ADMIN, COACH, STUDENT.

3. **ValidationException missing** — CLAUDE.md requires it; not yet created. Needs class + GlobalExceptionHandler entry.

4. **Smoke test blocked** — cannot test any secured endpoint until auth works and seed users exist with known credentials.

---

## What Claude will do with this context

After you paste the file contents back:

1. Diagnose the exact auth failure from `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`, and `AuthServiceImpl`
2. Generate a working Flyway migration (`V3__seed_users.sql` or similar) with real BCrypt hashes for 3 users
3. Add `ValidationException` class + handler entry
4. Provide a corrected smoke-test table with the exact credentials from the seed file
