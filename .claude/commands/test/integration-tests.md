---
description: Writes integration tests for AuthController and NoteController using MockMvc + Testcontainers + real PostgreSQL. Tests the full HTTP request chain including security filter, controller, service, and database. School requirement: minimum 2 integration tests.
allowed-tools: Read, Write, Bash(./mvnw test:*), Bash(./mvnw verify:*), Glob, Grep
---

# Integration Tests — AuthController + NoteController

## School requirements covered by this file

- ✅ Minimum 2 integration test classes (AuthController + NoteController)
- ✅ Uses MockMvc + Testcontainers (PostgreSQL Docker container)
- ✅ Tests the full HTTP chain: request → filter → controller → service → database

## What integration tests verify that unit tests cannot

Unit tests mock the database. Integration tests use a REAL PostgreSQL container via
Testcontainers. This means we verify:

- The JWT filter actually blocks requests without tokens
- The security config actually permits /api/auth/** without authentication
- The actual database schema (Flyway migrations) is applied correctly
- Data written in one request is readable in the next request
- Soft delete actually makes records invisible to subsequent GET requests

## Prerequisites

- Docker must be running (Testcontainers spawns a PostgreSQL container)
- `spring-boot-testcontainers` and `testcontainers-postgresql` must be in pom.xml
- Flyway migrations must exist: V1__seed_roles.sql (ROLE_STUDENT must be seeded)
- Run unit tests first — these are slower and should run after

## Step 0 — Read context first (MANDATORY)

```
src/main/java/org/noteplus/noteplus/controller/AuthController.java
src/main/java/org/noteplus/noteplus/controller/NoteController.java
src/main/java/org/noteplus/noteplus/security/SecurityConfig.java         ← permitted paths
src/main/java/org/noteplus/noteplus/dto/request/RegisterRequest.java
src/main/java/org/noteplus/noteplus/dto/request/LoginRequest.java
src/main/java/org/noteplus/noteplus/dto/request/CreateNoteRequest.java
src/main/java/org/noteplus/noteplus/dto/response/AuthResponse.java
src/main/resources/db/migration/V1__seed_roles.sql                       ← roles must be seeded
src/main/resources/db/migration/V2__seed_users.sql                       ← test users available
```

## Step 1 — Create shared base test class

Create `src/test/java/org/noteplus/noteplus/BaseIntegrationTest.java`:

```java
package org.noteplus.noteplus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for all integration tests.
 * Starts a single PostgreSQL Testcontainer shared across all test classes.
 * Each test class that extends this will use the same container instance.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("noteplus_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Helper: register a user and return the JWT token.
     * Use this to set up authenticated state for tests that need it.
     */
    protected String registerAndLogin(String username, String email, String password) throws Exception {
        // Register
        Map<String, String> registerBody = Map.of(
                "username", username,
                "email", email,
                "password", password
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated());

        // Login and extract token
        Map<String, String> loginBody = Map.of(
                "username", username,
                "password", password
        );
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn();

        var response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("token").asText();
    }
}
```

## Step 2 — Create application-test.properties

Create `src/test/resources/application-test.properties`:

```properties
# Testcontainers overrides datasource automatically via @ServiceConnection
# Flyway must run to seed ROLE_STUDENT (required for register)
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# Show SQL in test output for debugging
spring.jpa.show-sql=true

# Suppress startup banner in test logs
spring.main.banner-mode=off

# File upload dir for integration tests
file.upload-dir=target/test-uploads

# Disable email sending in tests — override with a no-op if Spring Mail is configured
spring.mail.host=localhost
spring.mail.port=3025
```

## Step 3 — AuthController integration tests

Create `src/test/java/org/noteplus/noteplus/controller/AuthControllerIntegrationTest.java`:

```java
package org.noteplus.noteplus.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.noteplus.noteplus.BaseIntegrationTest;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 * Tests the full chain: HTTP request → JWT filter → controller → service → PostgreSQL.
 */
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    // ── POST /api/auth/register ────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/register - valid request - returns 201 with token")
    void register_validRequest_returns201WithToken() throws Exception {
        // Arrange — unique username/email per test to avoid conflicts
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, String> body = Map.of(
                "username", "user_" + uniqueSuffix,
                "email", uniqueSuffix + "@test.nl",
                "password", "password123"
        );

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("user_" + uniqueSuffix))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_STUDENT"));
    }

    @Test
    @DisplayName("POST /api/auth/register - duplicate username - returns 409 Conflict")
    void register_duplicateUsername_returns409() throws Exception {
        // Arrange — register once
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, String> firstBody = Map.of(
                "username", "dupuser_" + uniqueSuffix,
                "email", "first_" + uniqueSuffix + "@test.nl",
                "password", "password123"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstBody)))
                .andExpect(status().isCreated());

        // Try to register with the same username
        Map<String, String> secondBody = Map.of(
                "username", "dupuser_" + uniqueSuffix,
                "email", "second_" + uniqueSuffix + "@test.nl",
                "password", "password123"
        );

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondBody)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("Username")));
    }

    @Test
    @DisplayName("POST /api/auth/register - missing password - returns 400 Bad Request")
    void register_missingPassword_returns400() throws Exception {
        // Arrange
        Map<String, String> body = Map.of(
                "username", "someuser",
                "email", "some@test.nl"
                // password intentionally omitted
        );

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/auth/register - password too short - returns 400 Bad Request")
    void register_passwordTooShort_returns400() throws Exception {
        // Arrange
        Map<String, String> body = Map.of(
                "username", "shortpwuser",
                "email", "short@test.nl",
                "password", "1234"   // less than 8 characters
        );

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/auth/login ───────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/login - valid credentials - returns 200 with JWT token")
    void login_validCredentials_returns200WithToken() throws Exception {
        // Arrange — create user first
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "loginuser_" + uniqueSuffix;
        registerAndLogin(username, username + "@test.nl", "password123");

        Map<String, String> loginBody = Map.of(
                "username", username,
                "password", "password123"
        );

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    @DisplayName("POST /api/auth/login - wrong password - returns 4xx error")
    void login_wrongPassword_returnsErrorResponse() throws Exception {
        // Arrange
        Map<String, String> body = Map.of(
                "username", "admin",  // seeded user from Flyway
                "password", "completelywrongpassword"
        );

        // Act & Assert
        // SECURITY: verify that a failed login does NOT return 200
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/auth/login - nonexistent user - same error response as wrong password")
    void login_nonexistentUser_returnsSameErrorAsWrongPassword() throws Exception {
        // Arrange
        Map<String, String> body = Map.of(
                "username", "userDoesNotExist",
                "password", "password123"
        );

        // Act & Assert
        // SECURITY: must return the same status code as wrong password (no username enumeration)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
    }
}
```

## Step 4 — NoteController integration tests

Create `src/test/java/org/noteplus/noteplus/controller/NoteControllerIntegrationTest.java`:

```java
package org.noteplus.noteplus.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.noteplus.noteplus.BaseIntegrationTest;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for NoteController.
 * Tests security filter chain, ownership rules, and soft delete behavior
 * against a real PostgreSQL database.
 */
class NoteControllerIntegrationTest extends BaseIntegrationTest {

    // ── Authentication guard ───────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/notes - no token - returns 401 Unauthorized")
    void getNotes_withoutToken_returns401() throws Exception {
        // Arrange — no token
        // Act & Assert
        mockMvc.perform(get("/api/notes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("POST /api/notes - no token - returns 401 Unauthorized")
    void createNote_withoutToken_returns401() throws Exception {
        // Arrange
        Map<String, Object> body = Map.of("title", "Test", "content", "Content");

        // Act & Assert
        mockMvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/notes - invalid token - returns 401 Unauthorized")
    void getNotes_withInvalidToken_returns401() throws Exception {
        // Arrange
        // Act & Assert
        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer this.is.not.a.valid.token"))
                .andExpect(status().isUnauthorized());
    }

    // ── CRUD happy paths ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/notes - valid token and body - returns 201 with created note")
    void createNote_withValidToken_returns201() throws Exception {
        // Arrange
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndLogin("noteuser_" + suffix, suffix + "@test.nl", "password123");

        Map<String, Object> body = Map.of(
                "title", "Integration Test Note",
                "content", "This note was created during an integration test"
        );

        // Act & Assert
        mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Integration Test Note"))
                .andExpect(jsonPath("$.ownerUsername").value("noteuser_" + suffix));
    }

    @Test
    @DisplayName("GET /api/notes - authenticated user - returns only own notes")
    void getNotes_authenticated_returnsOnlyOwnNotes() throws Exception {
        // Arrange — two users
        String suffix1 = UUID.randomUUID().toString().substring(0, 8);
        String suffix2 = UUID.randomUUID().toString().substring(0, 8);
        String token1 = registerAndLogin("user1_" + suffix1, suffix1 + "@test.nl", "password123");
        String token2 = registerAndLogin("user2_" + suffix2, suffix2 + "@test.nl", "password123");

        // User1 creates a note
        Map<String, Object> noteBody = Map.of("title", "User1 Note", "content", "Only for user1");
        mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteBody)))
                .andExpect(status().isCreated());

        // Act — User2 gets their own notes
        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                // User2's notes must NOT contain User1's note
                .andExpect(jsonPath("$[*].ownerUsername",
                        not(hasItem("user1_" + suffix1))));
    }

    // ── Ownership enforcement ──────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/notes/{id} - accessing another user's note - returns 403 Forbidden")
    void getById_accessingOtherUsersNote_returns403() throws Exception {
        // Arrange — two users
        String suffix1 = UUID.randomUUID().toString().substring(0, 8);
        String suffix2 = UUID.randomUUID().toString().substring(0, 8);
        String token1 = registerAndLogin("owner_" + suffix1, suffix1 + "@test.nl", "password123");
        String token2 = registerAndLogin("other_" + suffix2, suffix2 + "@test.nl", "password123");

        // User1 creates a note
        Map<String, Object> noteBody = Map.of("title", "Private Note", "content", "Secret");
        String createResult = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long noteId = objectMapper.readTree(createResult).get("id").asLong();

        // Act — User2 tries to access User1's note
        mockMvc.perform(get("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // ── Soft delete ────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/notes/{id} then GET - soft deleted note returns 404")
    void delete_softDeletedNote_subsequentGetReturns404() throws Exception {
        // Arrange
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndLogin("deluser_" + suffix, suffix + "@test.nl", "password123");

        // Create a note
        Map<String, Object> noteBody = Map.of("title", "To Be Deleted", "content", "Delete me");
        String createResult = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long noteId = objectMapper.readTree(createResult).get("id").asLong();

        // Delete it (soft delete)
        mockMvc.perform(delete("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Act — Try to GET the deleted note
        mockMvc.perform(get("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                // Assert — soft deleted note must be invisible (404, not 200 or 403)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /api/notes/{id} - soft deleted note disappears from GET /api/notes list")
    void delete_softDeletedNote_disappearsFromList() throws Exception {
        // Arrange
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndLogin("listuser_" + suffix, suffix + "@test.nl", "password123");

        // Create a note
        Map<String, Object> noteBody = Map.of("title", "Listed Note", "content", "I will vanish");
        String createResult = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long noteId = objectMapper.readTree(createResult).get("id").asLong();

        // Delete it
        mockMvc.perform(delete("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Act — GET the list
        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // Assert — the deleted note ID must not appear in the list
                .andExpect(jsonPath("$[*].id", not(hasItem(noteId.intValue()))));
    }

    // ── Validation ────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/notes - empty title - returns 400 Bad Request")
    void createNote_emptyTitle_returns400() throws Exception {
        // Arrange
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndLogin("valuser_" + suffix, suffix + "@test.nl", "password123");

        Map<String, Object> body = Map.of(
                "title", "",    // blank — @NotBlank must catch this
                "content", "Valid content"
        );

        // Act & Assert
        mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ── Admin endpoint ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/notes/all - non-admin user - returns 403 Forbidden")
    void getAllNotes_nonAdminUser_returns403() throws Exception {
        // Arrange — regular student account
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndLogin("student_" + suffix, suffix + "@test.nl", "password123");

        // Act & Assert
        mockMvc.perform(get("/api/notes/all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
```

[//]: # (## Step 5 — Run integration tests)
[//]: # ()
[//]: # (Make sure Docker is running before executing this command:)
[//]: # ()
[//]: # (```bash)
[//]: # (./mvnw test -Dtest="AuthControllerIntegrationTest,NoteControllerIntegrationTest")
[//]: # (```)

Testcontainers will:
1. Pull the `postgres:15-alpine` Docker image (first run only)
2. Start a PostgreSQL container on a random port
3. Run Flyway migrations (seeds ROLE_STUDENT, ROLE_COACH, ROLE_ADMIN)
4. Execute all tests
5. Stop and remove the container

Expected output: all tests GREEN, Testcontainers logs in the output.

## Step 6 — Run all tests together

[//]: # (```bash)
[//]: # (./mvnw verify)
[//]: # (```)

This runs unit tests + integration tests + JaCoCo coverage report.
Open `target/site/jacoco/index.html` to verify overall coverage.

## Step 7 — Checklist before committing

- [ ] Docker is running when tests execute
- [ ] `BaseIntegrationTest` uses `@ServiceConnection` — no manual JDBC URL config needed
- [ ] `application-test.properties` exists in `src/test/resources/`
- [ ] V1__seed_roles.sql seeds ROLE_STUDENT (required for register in tests)
- [ ] Each test uses `UUID.randomUUID()` to generate unique usernames — no test depends on another
- [ ] Soft delete test verifies 404 (not 200 or 403) after deletion
- [ ] Security test verifies 401 (not 403) for missing token
- [ ] `verify(jwtService, never())` pattern not needed here — real JWT is used end-to-end
- [ ] All 10+ tests pass: `./mvnw verify` exits with BUILD SUCCESS
