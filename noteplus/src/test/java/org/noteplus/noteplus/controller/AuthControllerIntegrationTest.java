package org.noteplus.noteplus.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.noteplus.noteplus.BaseIntegrationTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends BaseIntegrationTest {

    // ── POST /api/auth/register ──────────────────────────────────────────────

    @Test
    @DisplayName("register - valid body - 201 with non-empty token, correct username, roles contains ROLE_STUDENT")
    void register_validBody_returns201WithTokenAndStudentRole() throws Exception {
        // Arrange
        String s = UUID.randomUUID().toString().substring(0, 8);
        var body = Map.of("username", "user_" + s, "email", s + "@test.com", "password", "password123");

        // Act
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        // Assert
        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("token").asText()).isNotEmpty();
        assertThat(json.get("username").asText()).isEqualTo("user_" + s);
        assertThat(json.get("roles").toString()).contains("ROLE_STUDENT");
    }

    @Test
    @DisplayName("register - duplicate username - second call returns 409 with status=409 and message containing 'Username'")
    void register_duplicateUsername_returns409WithDuplicateMessage() throws Exception {
        // Arrange
        String s = UUID.randomUUID().toString().substring(0, 8);
        var first = Map.of("username", "dup_" + s, "email", s + "@test.com", "password", "password123");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        var second = Map.of("username", "dup_" + s, "email", "other_" + s + "@test.com", "password", "password123");

        // Act
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andReturn();

        // Assert
        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("status").asInt()).isEqualTo(409);
        assertThat(json.get("message").asText()).contains("Username");
    }

    @Test
    @DisplayName("register - password shorter than 8 characters - returns 400 with status=400")
    void register_passwordTooShort_returns400() throws Exception {
        // Arrange
        String s = UUID.randomUUID().toString().substring(0, 8);
        var body = Map.of("username", "user_" + s, "email", s + "@test.com", "password", "short");

        // Act
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Assert
        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("status").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("register - username shorter than 3 characters - returns 400")
    void register_usernameTooShort_returns400() throws Exception {
        // Arrange
        String s = UUID.randomUUID().toString().substring(0, 8);
        var body = Map.of("username", "ab", "email", s + "@test.com", "password", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/auth/login ─────────────────────────────────────────────────

    @Test
    @DisplayName("login - valid credentials (register first then login) - 200 with non-empty token and correct username")
    void login_validCredentials_returns200WithTokenAndUsername() throws Exception {
        // Arrange
        String s = UUID.randomUUID().toString().substring(0, 8);
        String username = "user_" + s;
        String email = s + "@test.com";
        registerAndLogin(username, email, "password123");

        var loginBody = Map.of("username", username, "password", "password123");

        // Act
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn();

        // Assert
        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("token").asText()).isNotEmpty();
        assertThat(json.get("username").asText()).isEqualTo(username);
    }

    @Test
    @DisplayName("login - wrong password - returns 4xx (must never be 200)")
    void login_wrongPassword_returns4xx() throws Exception {
        // Arrange
        String s = UUID.randomUUID().toString().substring(0, 8);
        registerAndLogin("user_" + s, s + "@test.com", "password123");
        var loginBody = Map.of("username", "user_" + s, "password", "wrongPassword!");

        // Act
        int status = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginBody)))
                .andReturn().getResponse().getStatus();

        // Assert
        assertThat(status).isGreaterThanOrEqualTo(400).isLessThan(500);
    }

    @Test
    @DisplayName("login - nonexistent user - same 4xx class as wrong password (no user enumeration)")
    void login_nonexistentUser_returns4xxSameAsWrongPassword() throws Exception {
        // Arrange
        var loginBody = Map.of("username", "ghost_" + UUID.randomUUID().toString().substring(0, 8), "password", "password123");

        // Act
        int status = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginBody)))
                .andReturn().getResponse().getStatus();

        // Assert
        assertThat(status).isGreaterThanOrEqualTo(400).isLessThan(500);
    }
}