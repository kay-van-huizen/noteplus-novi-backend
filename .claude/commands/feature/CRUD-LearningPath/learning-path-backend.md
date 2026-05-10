---
description: Builds the LearningPath backend — entity check, repository, DTOs, service interface, serviceImpl with dual-user ownership, note linking endpoints, and REST controller with role-based access for NotePlus.
allowed-tools: Read, Write, Bash(git checkout:*), Bash(git pull:*), Bash(./mvnw compile:*), Glob, Grep
---

# LearningPath Backend — NotePlus

## Business rules

A LearningPath always has two users:
- `student` — the student the path is designed for
- `coach`   — the coach responsible for the path

When a COACH creates a path → coach = self, student = assigned by coach (required).
When a STUDENT creates a path → student = self, coach = assigned by student (required).

Both student and coach can see and update the learning path.
Only ADMIN and COACH can delete a learning path.
A STUDENT cannot delete a learning path.

Note linking is handled via separate endpoints after the path is created.

| Action | ADMIN | COACH | STUDENT |
|--------|-------|-------|---------|
| GET own learning paths | ✅ | ✅ | ✅ |
| GET all learning paths | ✅ | ❌ | ❌ |
| POST create path | ✅ | ✅ | ✅ |
| PUT update path | ✅ | ✅ (own) | ✅ (own) |
| DELETE path | ✅ | ✅ (own) | ❌ |
| POST add note to path | ✅ | ✅ (own) | ✅ (own) |
| DELETE remove note from path | ✅ | ✅ (own) | ✅ (own) |

## Step 0 — Read context first (MANDATORY)

Before writing any code, read ALL of these:

```
src/main/java/org/noteplus/noteplus/entity/LearningPath.java     ← check existing fields
src/main/java/org/noteplus/noteplus/entity/Note.java             ← M:M relationship
src/main/java/org/noteplus/noteplus/entity/User.java             ← for dual-user FKs
src/main/java/org/noteplus/noteplus/entity/BaseEntity.java       ← id type (Long IDENTITY)
src/main/java/org/noteplus/noteplus/repository/UserRepository.java
src/main/java/org/noteplus/noteplus/repository/NoteRepository.java
src/main/java/org/noteplus/noteplus/service/NoteService.java
src/main/java/org/noteplus/noteplus/service/impl/NoteServiceImpl.java
src/main/java/org/noteplus/noteplus/controller/NoteController.java
src/main/java/org/noteplus/noteplus/exception/ForbiddenException.java
src/main/java/org/noteplus/noteplus/exception/ResourceNotFoundException.java
CLAUDE.md
```

## Step 2 — Verify and update the LearningPath entity

Read `LearningPath.java` carefully. The entity currently likely has a single `user` FK.
It needs TWO user relationships: one for the student and one for the coach.

If the entity does not already have both, update it as follows.
Do NOT delete existing fields — only ADD what is missing.

The entity should contain:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "student_id", nullable = false)
private User student;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "coach_id", nullable = false)
private User coach;

@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "learning_path_notes",
    joinColumns = @JoinColumn(name = "learning_path_id"),
    inverseJoinColumns = @JoinColumn(name = "note_id")
)
private List<Note> notes = new ArrayList<>();
```

If the entity already has a `user` field that does not map to either student or coach:
- Rename it to `student` if it represents the student
- Add a new `coach` field alongside it
- Update the column name in @JoinColumn accordingly

## Step 3 — Repository

Create `src/main/java/org/noteplus/noteplus/repository/LearningPathRepository.java`:

```java
package org.noteplus.noteplus.repository;

import org.noteplus.noteplus.entity.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {

    // All paths where the user is either the student or the coach
    @Query("SELECT lp FROM LearningPath lp WHERE lp.student.username = :username OR lp.coach.username = :username")
    List<LearningPath> findAllByUsername(String username);

    // Paths assigned to a specific student
    List<LearningPath> findByStudentUsername(String username);

    // Paths created/managed by a specific coach
    List<LearningPath> findByCoachUsername(String username);
}
```

## Step 4 — DTOs

Read LearningPath.java for exact field names before writing these records.

`src/main/java/org/noteplus/noteplus/dto/request/CreateLearningPathRequest.java`:
```java
package org.noteplus.noteplus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLearningPathRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    String title,

    String description,   // nullable

    @NotNull(message = "A student must be assigned to this learning path")
    Long studentId,       // required — the student this path is for

    @NotNull(message = "A coach must be assigned to this learning path")
    Long coachId          // required — the coach responsible
) {}
```

`src/main/java/org/noteplus/noteplus/dto/request/UpdateLearningPathRequest.java`:
```java
package org.noteplus.noteplus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLearningPathRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    String title,

    String description    // nullable
) {}
```

`src/main/java/org/noteplus/noteplus/dto/response/LearningPathResponse.java`:
```java
package org.noteplus.noteplus.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record LearningPathResponse(
    Long id,
    String title,
    String description,
    String studentUsername,
    String coachUsername,
    List<NoteResponse> notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

## Step 5 — Service interface

`src/main/java/org/noteplus/noteplus/service/LearningPathService.java`:

```java
package org.noteplus.noteplus.service;

import org.noteplus.noteplus.dto.request.CreateLearningPathRequest;
import org.noteplus.noteplus.dto.request.UpdateLearningPathRequest;
import org.noteplus.noteplus.dto.response.LearningPathResponse;

import java.util.List;

public interface LearningPathService {
    LearningPathResponse create(CreateLearningPathRequest request, String username);
    LearningPathResponse getById(Long id, String username);
    List<LearningPathResponse> getAllForUser(String username);  // student or coach — own paths
    List<LearningPathResponse> getAll();                        // ADMIN only
    LearningPathResponse update(Long id, UpdateLearningPathRequest request, String username);
    void delete(Long id, String username);
    LearningPathResponse addNote(Long learningPathId, Long noteId, String username);
    LearningPathResponse removeNote(Long learningPathId, Long noteId, String username);
}
```

## Step 6 — Service implementation

`src/main/java/org/noteplus/noteplus/service/impl/LearningPathServiceImpl.java`:

- `@Service @RequiredArgsConstructor`
- Inject: `LearningPathRepository`, `UserRepository`, `NoteRepository`

**create():**
- Load requesting user by username → throw ResourceNotFoundException("User not found") if absent
- Load student by studentId → throw ResourceNotFoundException("Student not found") if absent
- Load coach by coachId → throw ResourceNotFoundException("Coach not found") if absent
- Build LearningPath — set title, description, student, coach
- Save and return toResponse(saved)

**getById():**
- Load by id → throw ResourceNotFoundException("Learning path not found: " + id) if absent
- Access check: caller must be the student, the coach, or ADMIN
  → if NOT (lp.getStudent().getUsername().equals(username) OR lp.getCoach().getUsername().equals(username))
  → throw ForbiddenException("You do not have access to this learning path")
- Return toResponse(learningPath)

**getAllForUser():**
- Return learningPathRepository.findAllByUsername(username) mapped to toResponse()
- This returns paths where the user is either student or coach

**getAll():**
- Admin only — return learningPathRepository.findAll() mapped to toResponse()

**update():**
- Load by id → throw ResourceNotFoundException if absent
- Access check: caller must be student or coach of this path
  → throw ForbiddenException if not participant
- Apply title and description
- Save and return toResponse()

**delete():**
- Load by id → throw ResourceNotFoundException if absent
- Role check must happen in the controller via @PreAuthorize
- Ownership check here: caller must be the coach or ADMIN
  → if NOT lp.getCoach().getUsername().equals(username) → throw ForbiddenException("Only the coach or admin can delete a learning path")
- Delete (hard delete — LearningPath has no soft delete)

**addNote():**
- Load learning path by id → throw ResourceNotFoundException if absent
- Access check: caller must be student or coach of this path
- Load note by noteId → throw ResourceNotFoundException("Note not found") if absent
- Check note is not already in the list (prevent duplicates)
- lp.getNotes().add(note)
- Save and return toResponse()

**removeNote():**
- Load learning path → throw ResourceNotFoundException if absent
- Access check: caller must be student or coach
- Remove note from lp.getNotes() — use removeIf(n -> n.getId().equals(noteId))
- If nothing was removed → throw ResourceNotFoundException("Note not in this learning path")
- Save and return toResponse()

**private LearningPathResponse toResponse(LearningPath lp):**
```java
return new LearningPathResponse(
    lp.getId(),
    lp.getTitle(),
    lp.getDescription(),
    lp.getStudent().getUsername(),
    lp.getCoach().getUsername(),
    lp.getNotes().stream().map(this::noteToResponse).toList(),
    lp.getCreatedAt(),
    lp.getUpdatedAt()
);
```

For `noteToResponse`, map the Note fields to NoteResponse.
Check the exact NoteResponse constructor by reading NoteResponse.java first.

## Step 7 — REST Controller

`src/main/java/org/noteplus/noteplus/controller/LearningPathController.java`:

```java
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
            @PathVariable Long id,
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
            @PathVariable Long id,
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
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        learningPathService.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/notes/{noteId}")
    @Operation(summary = "Add a note to a learning path")
    @ApiResponse(responseCode = "200", description = "Note added to learning path")
    @ApiResponse(responseCode = "403", description = "Not a participant of this path")
    @ApiResponse(responseCode = "404", description = "Learning path or note not found")
    public ResponseEntity<LearningPathResponse> addNote(
            @PathVariable Long id,
            @PathVariable Long noteId,
            Authentication auth) {
        return ResponseEntity.ok(learningPathService.addNote(id, noteId, auth.getName()));
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    @Operation(summary = "Remove a note from a learning path")
    @ApiResponse(responseCode = "200", description = "Note removed from learning path")
    @ApiResponse(responseCode = "403", description = "Not a participant of this path")
    @ApiResponse(responseCode = "404", description = "Learning path or note not found")
    public ResponseEntity<LearningPathResponse> removeNote(
            @PathVariable Long id,
            @PathVariable Long noteId,
            Authentication auth) {
        return ResponseEntity.ok(learningPathService.removeNote(id, noteId, auth.getName()));
    }
}
```

## Step 8 — Compile and smoke test

Verify in Swagger:

| Test | Token role | Expected |
|------|-----------|----------|
| POST /api/learning-paths (with studentId + coachId) | COACH token | 201 Created |
| POST /api/learning-paths (with studentId + coachId) | STUDENT token | 201 Created |
| POST /api/learning-paths without coachId | STUDENT token | 400 Bad Request |
| GET /api/learning-paths | STUDENT token | 200 — own paths |
| GET /api/learning-paths/all | STUDENT token | 403 Forbidden |
| GET /api/learning-paths/all | ADMIN token | 200 — all paths |
| GET /api/learning-paths/{id} — not participant | any token | 403 Forbidden |
| DELETE /api/learning-paths/{id} | STUDENT token | 403 Forbidden |
| DELETE /api/learning-paths/{id} (own) | COACH token | 204 No Content |
| POST /api/learning-paths/{id}/notes/{noteId} | COACH token | 200 with notes list |
| DELETE /api/learning-paths/{id}/notes/{noteId} | STUDENT token | 200 updated |

Do NOT proceed to learning-path-frontend.md until all tests pass.
