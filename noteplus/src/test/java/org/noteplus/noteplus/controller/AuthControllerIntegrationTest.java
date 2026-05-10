package org.noteplus.noteplus.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.noteplus.noteplus.BaseIntegrationTest;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 * Covers the full HTTP chain: request → JWT filter → controller → service → PostgreSQL.
 */
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    // ── POST /api/auth/register ────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/register - valid request - returns 201 with token and ROLE_STUDENT")
    void register_validRequest_returns201WithToken() throws Exception {
        // Arrange
        // RegisterRequest fields: username, email, password (no name field)
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, String> body = new HashMap<>();
        body.put("username", "user_" + suffix);
        body.put("email", suffix + "@test.nl");
        body.put("password", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("user_" + suffix))
                .andExpect(jsonPath("$.roles").isArray())
                // hasItem: order-safe — roles is backed by a Set internally
                .andExpect(jsonPath("$.roles", hasItem("ROLE_STUDENT")));
    }

    @Test
    @DisplayName("POST /api/auth/register - duplicate username - returns 409 Conflict")
    void register_duplicateUsername_returns409() throws Exception {
        // Arrange — register once successfully
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, String> first = new HashMap<>();
        first.put("username", "dupuser_" + suffix);
        first.put("email", "first_" + suffix + "@test.nl");
        first.put("password", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        // Try again with same username, different email
        Map<String, String> second = new HashMap<>();
        second.put("username", "dupuser_" + suffix);    // same username → conflict
        second.put("email", "second_" + suffix + "@test.nl");
        second.put("password", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("Username")));
    }

    @Test
    @DisplayName("POST /api/auth/register - password too short - returns 400 Bad Request")
    void register_passwordTooShort_returns400() throws Exception {
        // Arrange
        Map<String, String> body = new HashMap<>();
        body.put("username", "shortpw");
        body.put("email", "shortpw@test.nl");
        body.put("password", "1234");   // less than 8 characters → @Size validation fails

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/auth/register - username too short - returns 400 Bad Request")
    void register_usernameTooShort_returns400() throws Exception {
        // Arrange
        Map<String, String> body = new HashMap<>();
        body.put("username", "ab");     // min length is 3 → @Size validation fails
        body.put("email", "ab@test.nl");
        body.put("password", "password123");

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
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "loginuser_" + suffix;
        registerAndLogin(username, username + "@test.nl", "password123");

        Map<String, String> loginBody = new HashMap<>();
        loginBody.put("username", username);
        loginBody.put("password", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    @DisplayName("POST /api/auth/login - wrong password - returns 4xx error")
    void login_wrongPassword_returnsErrorResponse() throws Exception {
        // Arrange
        Map<String, String> body = new HashMap<>();
        body.put("username", "student1");   // seeded in V2__seed_users.sql
        body.put("password", "completelywrongpassword");

        // Act & Assert — a failed login must NEVER return 200
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/auth/login - nonexistent user - same 4xx response class as wrong password")
    void login_nonexistentUser_returnsSameErrorClassAsWrongPassword() throws Exception {
        // Arrange
        Map<String, String> body = new HashMap<>();
        body.put("username", "userDoesNotExist");
        body.put("password", "password123");

        // Act & Assert — SECURITY: must not reveal that the username doesn't exist
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
    }
}
