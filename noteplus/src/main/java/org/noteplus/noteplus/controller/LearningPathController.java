package org.noteplus.noteplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.CreateLearningPathRequest;
import org.noteplus.noteplus.dto.request.UpdateLearningPathRequest;
import org.noteplus.noteplus.dto.response.LearningPathResponse;
import org.noteplus.noteplus.service.LearningPathService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/learning-paths")
@RequiredArgsConstructor
@Tag(name = "Learning Paths", description = "Learning path management — Coach assigns paths to students, students can also create their own")
@SecurityRequirement(name = "bearerAuth")
public class LearningPathController {

    private final LearningPathService learningPathService;

    @GetMapping
    @Operation(summary = "Get all learning paths for the current user (as student or coach)")
    @ApiResponse(responseCode = "200", description = "Learning paths retrieved")
    public ResponseEntity<List<LearningPathResponse>> getMyPaths(Authentication auth) {
        return ResponseEntity.ok(learningPathService.getAllForUser(auth.getName()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all learning paths — ADMIN only")
    @ApiResponse(responseCode = "200", description = "All learning paths retrieved")
    @ApiResponse(responseCode = "403", description = "Admin role required")
    public ResponseEntity<List<LearningPathResponse>> getAll() {
        return ResponseEntity.ok(learningPathService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a learning path by ID — must be student, coach, or ADMIN")
    @ApiResponse(responseCode = "200", description = "Learning path found")
    @ApiResponse(responseCode = "403", description = "Not a participant of this path")
    @ApiResponse(responseCode = "404", description = "Learning path not found")
    public ResponseEntity<LearningPathResponse> getById(
            @PathVariable UUID id,
            Authentication auth) {
        return ResponseEntity.ok(learningPathService.getById(id, auth.getName()));
    }

    @PostMapping
    @Operation(summary = "Create a new learning path — must assign both student and coach")
    @ApiResponse(responseCode = "201", description = "Learning path created")
    @ApiResponse(responseCode = "400", description = "Validation error — studentId and coachId are required")
    public ResponseEntity<LearningPathResponse> create(
            @Valid @RequestBody CreateLearningPathRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(learningPathService.create(request, auth.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a learning path — must be student or coach of this path")
    @ApiResponse(responseCode = "200", description = "Learning path updated")
    @ApiResponse(responseCode = "403", description = "Not a participant of this path")
    @ApiResponse(responseCode = "404", description = "Learning path not found")
    public ResponseEntity<LearningPathResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLearningPathRequest request,
            Authentication auth) {
        return ResponseEntity.ok(learningPathService.update(id, request, auth.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COACH')")
    @Operation(summary = "Delete a learning path — COACH (own) or ADMIN only")
    @ApiResponse(responseCode = "204", description = "Learning path deleted")
    @ApiResponse(responseCode = "403", description = "Students cannot delete learning paths")
    @ApiResponse(responseCode = "404", description = "Learning path not found")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication auth) {
        learningPathService.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/notes/{noteId}")
    @Operation(summary = "Add a note to a learning path")
    @ApiResponse(responseCode = "200", description = "Note added to learning path")
    @ApiResponse(responseCode = "403", description = "Not a participant of this path")
    @ApiResponse(responseCode = "404", description = "Learning path or note not found")
    public ResponseEntity<LearningPathResponse> addNote(
            @PathVariable UUID id,
            @PathVariable UUID noteId,
            Authentication auth) {
        return ResponseEntity.ok(learningPathService.addNote(id, noteId, auth.getName()));
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    @Operation(summary = "Remove a note from a learning path")
    @ApiResponse(responseCode = "200", description = "Note removed from learning path")
    @ApiResponse(responseCode = "403", description = "Not a participant of this path")
    @ApiResponse(responseCode = "404", description = "Learning path or note not found")
    public ResponseEntity<LearningPathResponse> removeNote(
            @PathVariable UUID id,
            @PathVariable UUID noteId,
            Authentication auth) {
        return ResponseEntity.ok(learningPathService.removeNote(id, noteId, auth.getName()));
    }
}
