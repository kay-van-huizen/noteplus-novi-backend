package org.noteplus.noteplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.CreateNoteRequest;
import org.noteplus.noteplus.dto.request.UpdateNoteRequest;
import org.noteplus.noteplus.dto.response.NoteResponse;
import org.noteplus.noteplus.service.NoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@Tag(name = "Notes", description = "Note management — Student and Coach manage their own notes, Admin manages all")
@SecurityRequirement(name = "bearerAuth")
public class NoteController {

    private final NoteService noteService;

    @GetMapping
    @Operation(summary = "Get all notes for the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "Notes retrieved successfully")
    public ResponseEntity<List<NoteResponse>> getMyNotes(Authentication auth) {
        return ResponseEntity.ok(noteService.getAllForUser(auth.getName()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all notes from all users — ADMIN only")
    @ApiResponse(responseCode = "200", description = "All notes retrieved")
    @ApiResponse(responseCode = "403", description = "Admin role required")
    public ResponseEntity<List<NoteResponse>> getAll() {
        return ResponseEntity.ok(noteService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a note by ID — must be owner or ADMIN")
    @ApiResponse(responseCode = "200", description = "Note found")
    @ApiResponse(responseCode = "403", description = "Not your note")
    @ApiResponse(responseCode = "404", description = "Note not found or already deleted")
    public ResponseEntity<NoteResponse> getById(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(noteService.getById(id, auth.getName()));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get notes filtered by category")
    @ApiResponse(responseCode = "200", description = "Notes retrieved")
    public ResponseEntity<List<NoteResponse>> getByCategory(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(noteService.getByCategoryId(categoryId));
    }

    @PostMapping
    @Operation(summary = "Create a new note — all authenticated users")
    @ApiResponse(responseCode = "201", description = "Note created successfully")
    @ApiResponse(responseCode = "400", description = "Validation error")
    public ResponseEntity<NoteResponse> create(
            @Valid @RequestBody CreateNoteRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteService.create(request, auth.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a note — must be the owner")
    @ApiResponse(responseCode = "200", description = "Note updated successfully")
    @ApiResponse(responseCode = "403", description = "Not your note")
    @ApiResponse(responseCode = "404", description = "Note not found")
    public ResponseEntity<NoteResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNoteRequest request,
            Authentication auth) {
        return ResponseEntity.ok(noteService.update(id, request, auth.getName()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a note — must be owner or ADMIN")
    @ApiResponse(responseCode = "204", description = "Note soft-deleted successfully")
    @ApiResponse(responseCode = "403", description = "Not your note")
    @ApiResponse(responseCode = "404", description = "Note not found")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication auth) {
        noteService.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
