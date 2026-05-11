package org.noteplus.noteplus.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.noteplus.noteplus.BaseIntegrationTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NoteControllerIntegrationTest extends BaseIntegrationTest {

    // ── Authentication guard ─────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/notes without token - returns 401 with error='Unauthorized'")
    void getMyNotes_withoutToken_returns401WithUnauthorizedError() throws Exception {
        // Act & Assert
        MvcResult result = mockMvc.perform(get("/api/notes"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("Unauthorized");
    }

    @Test
    @DisplayName("POST /api/notes without token - returns 401")
    void createNote_withoutToken_returns401() throws Exception {
        // Arrange
        var body = Map.of("title", "Test", "content", "Content");

        // Act & Assert
        mockMvc.perform(post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/notes with invalid token - returns 401")
    void getMyNotes_withInvalidToken_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/notes")
                .header("Authorization", "Bearer this.is.not.a.valid.token"))
                .andExpect(status().isUnauthorized());
    }

    // ── CRUD happy paths ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/notes with valid token - returns 201 with id, correct title, and ownerUsername")
    void createNote_withValidToken_returns201WithNoteFields() throws Exception {
        // Arrange
        String s = UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndLogin("user_" + s, s + "@test.com", "password123");
        var body = Map.of("title", "My First Note", "content", "Some content here");

        // Act
        MvcResult result = mockMvc.perform(post("/api/notes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        // Assert
        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("id").asText()).isNotEmpty();
        assertThat(json.get("title").asText()).isEqualTo("My First Note");
        assertThat(json.get("ownerUsername").asText()).isEqualTo("user_" + s);
    }

    @Test
    @DisplayName("GET /api/notes authenticated - returns 200 with only current user's notes (user2 cannot see user1's notes)")
    void getMyNotes_authenticated_returnsOnlyOwnNotes() throws Exception {
        // Arrange
        String s1 = UUID.randomUUID().toString().substring(0, 8);
        String s2 = UUID.randomUUID().toString().substring(0, 8);
        String token1 = registerAndLogin("user1_" + s1, s1 + "@test.com", "password123");
        String token2 = registerAndLogin("user2_" + s2, s2 + "@test.com", "password123");

        mockMvc.perform(post("/api/notes")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "User1 Note", "content", "private"))))
                .andExpect(status().isCreated());

        // Act
        MvcResult result = mockMvc.perform(get("/api/notes")
                .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andReturn();

        // Assert
        assertThat(result.getResponse().getContentAsString()).doesNotContain("user1_" + s1);
    }

    // ── Ownership enforcement ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/notes/{id} as a different user - returns 403 with status=403")
    void getById_differentUser_returns403WithStatus() throws Exception {
        // Arrange
        String s1 = UUID.randomUUID().toString().substring(0, 8);
        String s2 = UUID.randomUUID().toString().substring(0, 8);
        String token1 = registerAndLogin("user1_" + s1, s1 + "@test.com", "password123");
        String token2 = registerAndLogin("user2_" + s2, s2 + "@test.com", "password123");

        MvcResult createResult = mockMvc.perform(post("/api/notes")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "Private Note", "content", "secret"))))
                .andExpect(status().isCreated())
                .andReturn();

        String noteId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        // Act
        MvcResult result = mockMvc.perform(get("/api/notes/" + noteId)
                .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden())
                .andReturn();

        // Assert
        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("status").asInt()).isEqualTo(403);
    }

    // ── Soft delete ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/notes/{id} then GET /api/notes/{id} - delete returns 204, subsequent GET returns 404 with status=404")
    void delete_thenGetById_returns204Then404() throws Exception {
        // Arrange
        String s = UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndLogin("user_" + s, s + "@test.com", "password123");

        MvcResult createResult = mockMvc.perform(post("/api/notes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "To Delete", "content", "bye"))))
                .andExpect(status().isCreated())
                .andReturn();

        String noteId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        // Act
        mockMvc.perform(delete("/api/notes/" + noteId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Assert
        MvcResult getResult = mockMvc.perform(get("/api/notes/" + noteId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andReturn();

        var json = objectMapper.readTree(getResult.getResponse().getContentAsString());
        assertThat(json.get("status").asInt()).isEqualTo(404);
    }

    @Test
    @DisplayName("DELETE /api/notes/{id} then GET /api/notes - deleted note's id no longer appears in the list")
    void delete_thenList_deletedNoteAbsentFromList() throws Exception {
        // Arrange
        String s = UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndLogin("user_" + s, s + "@test.com", "password123");

        MvcResult createResult = mockMvc.perform(post("/api/notes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("title", "Soft Delete Me", "content", "gone"))))
                .andExpect(status().isCreated())
                .andReturn();

        String noteId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(delete("/api/notes/" + noteId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Act
        MvcResult listResult = mockMvc.perform(get("/api/notes")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        // Assert
        assertThat(listResult.getResponse().getContentAsString()).doesNotContain(noteId);
    }

    // ── Validation ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/notes with empty title - returns 400 with status=400")
    void createNote_emptyTitle_returns400WithStatus() throws Exception {
        // Arrange
        String s = UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndLogin("user_" + s, s + "@test.com", "password123");
        var body = Map.of("title", "", "content", "Some content");

        // Act
        MvcResult result = mockMvc.perform(post("/api/notes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Assert
        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("status").asInt()).isEqualTo(400);
    }

    // ── Admin-only endpoint ──────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/notes/all as a student (registered via registerAndLogin) - returns 403")
    void getAll_asStudent_returns403() throws Exception {
        // Arrange
        String s = UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndLogin("student_" + s, s + "@test.com", "password123");

        // Act & Assert
        mockMvc.perform(get("/api/notes/all")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}