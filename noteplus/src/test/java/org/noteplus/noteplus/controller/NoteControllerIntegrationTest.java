package org.noteplus.noteplus.controller;

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
 * Tests the JWT filter, ownership enforcement, and soft delete against a real PostgreSQL database.
 */
class NoteControllerIntegrationTest extends BaseIntegrationTest {

    // ── Authentication guard ───────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/notes - no token - returns 401 Unauthorized")
    void getNotes_withoutToken_returns401() throws Exception {
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
        // Arrange — two separate users
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

        // Act — User2 fetches their own notes
        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                // User1's note must NOT appear in User2's list
                .andExpect(jsonPath("$[*].ownerUsername", not(hasItem("user1_" + suffix1))));
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
        Map<String, Object> noteBody = Map.of("title", "Private Note", "content", "Secret content");
        String createJson = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long noteId = objectMapper.readTree(createJson).get("id").asLong();

        // Act — User2 tries to read User1's note
        mockMvc.perform(get("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // ── Soft delete ────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/notes/{id} then GET /{id} - soft deleted note returns 404")
    void delete_softDeletedNote_subsequentGetReturns404() throws Exception {
        // Arrange
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndLogin("deluser_" + suffix, suffix + "@test.nl", "password123");

        Map<String, Object> noteBody = Map.of("title", "To Be Deleted", "content", "Delete me");
        String createJson = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long noteId = objectMapper.readTree(createJson).get("id").asLong();

        // Soft delete
        mockMvc.perform(delete("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Act — try to GET the deleted note
        mockMvc.perform(get("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                // Assert — soft deleted note must be invisible (not 200 or 403)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE /api/notes/{id} - soft deleted note disappears from GET /api/notes list")
    void delete_softDeletedNote_disappearsFromList() throws Exception {
        // Arrange
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndLogin("listuser_" + suffix, suffix + "@test.nl", "password123");

        Map<String, Object> noteBody = Map.of("title", "Listed Note", "content", "I will vanish");
        String createJson = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long noteId = objectMapper.readTree(createJson).get("id").asLong();

        // Soft delete
        mockMvc.perform(delete("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Act — fetch the list
        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // Assert — deleted note ID must not appear in the list
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
                "title", "",        // @NotBlank should reject this
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

    // ── Admin-only endpoint ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/notes/all - non-admin (student) user - returns 403 Forbidden")
    void getAllNotes_nonAdminUser_returns403() throws Exception {
        // Arrange — register returns a ROLE_STUDENT by default
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndLogin("student_" + suffix, suffix + "@test.nl", "password123");

        // Act & Assert
        mockMvc.perform(get("/api/notes/all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
