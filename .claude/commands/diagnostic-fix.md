---
description: Diagnoses and fixes build errors, Docker/Testcontainers setup, BaseIntegrationTest compile issues, and JTE frontend not loading. Run this before any other feature work.
allowed-tools: Read, Write, Bash(./mvnw compile:*), Bash(./mvnw spring-boot:run:*), Bash(./mvnw test:*), Bash(docker ps:*), Bash(docker info:*), Glob, Grep
---

# Diagnostic & Fix — Build, Docker, Integration Tests, JTE Frontend

## What this prompt investigates and fixes

1. Current compile and build status
2. Docker availability for Testcontainers
3. BaseIntegrationTest compile errors
4. JTE frontend not reachable (localhost:8080/login returns "site not reachable")

Work through each phase in order. Do not skip ahead.

---

## PHASE 1 — Compile status

Run:
```bash
./mvnw compile 2>&1
```

Read the full output. Categorize every error into one of:
- MISSING_DEPENDENCY — class not found, package does not exist
- WRONG_IMPORT — imported the wrong class (e.g. wrong AccessDeniedException)
- MISSING_METHOD — method does not exist on the class
- ENTITY_MISMATCH — field name used in code does not match entity definition

For each error, read the affected source file and fix it.
After fixing, run `./mvnw compile` again.
Repeat until the output ends with: `BUILD SUCCESS`

Do not proceed to Phase 2 until compile is clean.

---

## PHASE 2 — Identify why the app is not reachable

The symptom: http://localhost:8080/login returns "site not reachable".
This means the app is either not running or crashing on startup.

Run:
```bash
./mvnw spring-boot:run 2>&1
```

Read the startup log carefully. Look for:

**A) Port already in use:**
```
Web server failed to start. Port 8080 was already in use.
```
Fix: kill the process using port 8080
```bash
# Windows PowerShell:
netstat -ano | findstr :8080
taskkill /PID <pid> /F
```

**B) Database connection failure:**
```
Connection to localhost:5432 refused
HikariPool: Connection is not available
```
Fix: PostgreSQL is not running.
Instruct the user: start PostgreSQL via pgAdmin or Windows Services before running the app.
Check application.properties for correct datasource URL, username, password.

**C) Flyway migration failure:**
```
FlywayException: Found non-empty schema
Migration checksum mismatch
```
Fix options:
- If schema is empty: add `spring.flyway.baseline-on-migrate=true` to application.properties temporarily
- If migration scripts changed: run `./mvnw flyway:repair` or drop and recreate the database

**D) Bean creation failure:**
```
UnsatisfiedDependencyException
NoSuchBeanDefinitionException
```
Fix: read the full stack trace, identify which bean is missing, check if the class has @Service/@Repository/@Component annotation.

**E) JTE template not found:**
```
TemplateNotFoundException: categories/form
```
Fix: verify that `src/main/jte/` directory exists and contains the template files.
Check application.properties for `gg.jte.developmentMode=true`.

Report what error you found and apply the specific fix above.
After fixing, run `./mvnw spring-boot:run` again and confirm the app starts.

The startup success indicator is:
```
Started NoteplusApplication in X.XXX seconds
Tomcat started on port 8080
```

---

## PHASE 3 — Verify JTE frontend is reachable

After the app starts successfully:

Check that these paths are in SecurityConfig permitAll():
```java
"/login",
"/login/**",
"/register",
"/register/**",
"/categories/**",
"/notes/**",
"/settings/**",
"/learning-paths/**",
"/forgot-password",
"/reset-password"
```

Read SecurityConfig.java and verify each path is present.
If any are missing, add them and restart the app.

Then verify these JTE template files exist:
```
src/main/jte/auth/login.jte        ← for /login
src/main/jte/auth/register.jte     ← for /register
src/main/jte/categories/list.jte   ← for /categories
src/main/jte/categories/form.jte   ← for /categories/new
src/main/jte/notes/list.jte        ← for /notes
src/main/jte/notes/form.jte        ← for /notes/new
src/main/jte/settings/index.jte    ← for /settings
```

For each missing file, report which template is absent.

Also verify these ViewControllers exist and have the correct @GetMapping:
```
src/main/java/.../controller/AuthViewController.java      → @GetMapping("/login"), @GetMapping("/register")
src/main/java/.../controller/CategoryViewController.java  → @GetMapping("/categories")
src/main/java/.../controller/NoteViewController.java      → @GetMapping("/notes")
src/main/java/.../controller/SettingsViewController.java  → @GetMapping("/settings")
```

Report any missing controllers or missing mappings.

---

## PHASE 4 — Fix BaseIntegrationTest compile errors

Read `src/test/java/org/noteplus/noteplus/BaseIntegrationTest.java`.

Check pom.xml for this dependency:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

If missing, add it to pom.xml inside <dependencies>.

Then run:
```bash
./mvnw test-compile 2>&1
```

Common errors and fixes:

**@AutoConfigureMockMvc not found:**
→ spring-boot-starter-test is missing (fix above)

**PostgreSQLContainer not found:**
→ verify testcontainers-postgresql is in pom.xml:
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

**@ServiceConnection not found:**
→ verify spring-boot-testcontainers is in pom.xml:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

**@Testcontainers not found:**
→ verify testcontainers-junit-jupiter is in pom.xml:
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

After adding any missing dependencies, run Maven reload and then:
```bash
./mvnw test-compile 2>&1
```

Repeat until test-compile ends with BUILD SUCCESS.

---

## PHASE 5 — Docker check for Testcontainers

Run:
```bash
docker info 2>&1
```

**If Docker is running:** output shows Server Version, Containers count etc. → proceed.

**If Docker is not running:**
```
ERROR: Cannot connect to the Docker daemon
```
Fix: Docker Desktop must be started manually on Windows.
Instruct the user: open Docker Desktop from the Start menu and wait for it to fully start (whale icon in system tray stops animating).
Then re-run `docker info` to confirm.

**If Docker is not installed:**
Output: 'docker' is not recognized
Fix: instruct the user to download Docker Desktop from https://www.docker.com/products/docker-desktop
After installation, restart the computer and run `docker info` again.

After Docker is confirmed running, run a quick Testcontainers smoke test:
```bash
./mvnw test -Dtest=BaseIntegrationTest#*
```

If this fails with a timeout or container error, check:
- Docker Desktop has enough resources (Settings → Resources → min 2GB RAM)
- Windows Hyper-V or WSL2 is enabled (required for Docker Desktop on Windows)

---

## PHASE 6 — Final status report

After completing all phases, provide a clear report:

```
BUILD STATUS:         [PASS / FAIL]
APP STARTS:           [PASS / FAIL — include port and startup time]
LOGIN PAGE (/login):  [PASS / FAIL — reachable in browser]
REGISTER PAGE:        [PASS / FAIL]
TEST COMPILE:         [PASS / FAIL]
DOCKER RUNNING:       [YES / NO]
INTEGRATION TESTS:    [PASS / FAIL / SKIPPED — reason]

REMAINING ISSUES:
- [list any unresolved issues with suggested next step]
```

Do not mark anything as PASS unless you have actually verified it by running the command or checking the file.
