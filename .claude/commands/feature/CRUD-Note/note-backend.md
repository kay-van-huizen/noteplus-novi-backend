---
description: Builds the Note backend — repository, DTOs, service interface, serviceImpl with soft delete + ownership, and REST controller with role-based access for NotePlus.
allowed-tools: Read, Write, Bash(git checkout:*), Bash(git pull:*), Bash(./mvnw compile:*), Glob, Grep
---

# Note Backend — NotePlus

## Role rules for Note

A note belongs to the user who created it — Student or Coach.
Both roles can only see and manage their own notes.
Admin sees everything.

| Action | ADMIN | COACH | STUDENT |
|--------|-------|-------|---------|
| GET own notes | ✅ | ✅ | ✅ |
| GET all notes | ✅ | ❌ | ❌ |
| POST create note | ✅ | ✅ | ✅ |
| PUT update own note | ✅ | ✅ | ✅ |
| DELETE (soft) own note | ✅ | ✅ | ✅ |
| Access other user's note | ✅ | ❌ | ❌ |

## Step 0 — Read context first (MANDATORY)

Before writing a single line of code, read ALL of these:

```
src/main/java/org/noteplus/noteplus/entity/Note.java
src/main/java/org/noteplus/noteplus/entity/BaseEntity.java
src/main/java/org/noteplus/noteplus/entity/User.java
src/main/java/org/noteplus/noteplus/entity/Category.java
src/main/java/org/noteplus/noteplus/repository/UserRepository.java
src/main/java/org/noteplus/noteplus/repository/CategoryRepository.java
src/main/java/org/noteplus/noteplus/service/CategoryService.java
src/main/java/org/noteplus/noteplus/service/impl/CategoryServiceImpl.java
src/main/java/org/noteplus/noteplus/controller/CategoryController.java
src/main/java/org/noteplus/noteplus/exception/ForbiddenException.java
src/main/java/org/noteplus/noteplus/exception/ResourceNotFoundException.java
CLAUDE.md
```

Note has soft delete via `deletedAt` — understand this before building the repository queries.
Note has User ownership — every write operation needs an ownership check.
Both STUDENT and COACH own their own notes — the ownership model is identical for both roles.

## Step 1 — Git branch

```bash
git checkout main
git pull origin main
git checkout -b feat/note-crud
```

## Step 2 — Repository

Create `src/main/java/org/noteplus/noteplus/repository/NoteRepository.java`:

```java
package org.noteplus.noteplus.repository;

import org.noteplus.noteplus.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    // Own notes only — for STUDENT and COACH
    @Query("SELECT n FROM Note n WHERE n.user.username = :username AND n.deletedAt IS NULL")
    List<Note> findAllByUsernameNotDeleted(String username);

    // Single note — exclude soft-deleted
    @Query("SELECT n FROM Note n WHERE n.id = :id AND n.deletedAt IS NULL")
    Optional<Note> findByIdNotDeleted(Long id);

    // Admin only — all notes across all users
    @Query("SELECT n FROM Note n WHERE n.deletedAt IS NULL")
    List<Note> findAllNotDeleted();

    // Notes filtered by category
    @Query("SELECT n FROM Note n WHERE n.category.id = :categoryId AND n.deletedAt IS NULL")
    List<Note> findByCategoryIdNotDeleted(Long categoryId);
}
```

NEVER use findAll() directly — it returns soft-deleted notes.

## Step 3 — DTOs

Read the exact field names from Note.java before writing these records.

`src/main/java/org/noteplus/noteplus/dto/request/CreateNoteRequest.java`:
```java
package org.noteplus.noteplus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNoteRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    String title,

    @NotBlank(message = "Content is required")
    String content,

    Long categoryId   // nullable — a note does not require a category
) {}
```

`src/main/java/org/noteplus/noteplus/dto/request/UpdateNoteRequest.java`:
```java
package org.noteplus.noteplus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNoteRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    String title,

    @NotBlank(message = "Content is required")
    String content,

    Long categoryId   // nullable — can be unlinked from a note
) {}
```

`src/main/java/org/noteplus/noteplus/dto/response/NoteResponse.java`:
```java
package org.noteplus.noteplus.dto.response;

import java.time.LocalDateTime;

public record NoteResponse(
    Long id,
    String title,
    String content,
    String ownerUsername,
    Long categoryId,
    String categoryTitle,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

## Step 4 — Service interface

`src/main/java/org/noteplus/noteplus/service/NoteService.java`:

```java
package org.noteplus.noteplus.service;

import org.noteplus.noteplus.dto.request.CreateNoteRequest;
import org.noteplus.noteplus.dto.request.UpdateNoteRequest;
import org.noteplus.noteplus.dto.response.NoteResponse;

import java.util.List;

public interface NoteService {
    NoteResponse create(CreateNoteRequest request, String username);
    NoteResponse getById(Long id, String username);
    List<NoteResponse> getAllForUser(String username);   // STUDENT + COACH: own notes only
    List<NoteResponse> getAll();                         // ADMIN only
    List<NoteResponse> getByCategoryId(Long categoryId);
    NoteResponse update(Long id, UpdateNoteRequest request, String username);
    void delete(Long id, String username);               // soft delete — never hard delete
}
```

## Step 5 — Service implementation

`src/main/java/org/noteplus/noteplus/service/impl/NoteServiceImpl.java`:

- `@Service @RequiredArgsConstructor`
- Inject: `NoteRepository`, `UserRepository`, `CategoryRepository`

**create():**
- Load user by username → throw ResourceNotFoundException("User not found") if absent
- If categoryId is not null: load Category → throw ResourceNotFoundException("Category not found: " + categoryId) if absent
- Build Note entity — set title, content, user, category
- Save and return toResponse(saved)

**getById():**
- Use noteRepository.findByIdNotDeleted(id) → throw ResourceNotFoundException("Note not found: " + id) if absent
- Ownership check: if note.getUser().getUsername() does NOT equal username → throw ForbiddenException("You do not have access to this note")
- This check applies equally to STUDENT and COACH — neither can read the other's notes
- Return toResponse(note)

**getAllForUser():**
- Used by STUDENT, COACH, and ADMIN when viewing own notes
- Return noteRepository.findAllByUsernameNotDeleted(username) mapped to toResponse()

**getAll():**
- Admin-only — no ownership check needed here, the controller enforces the role
- Return noteRepository.findAllNotDeleted() mapped to toResponse()

**getByCategoryId():**
- Return noteRepository.findByCategoryIdNotDeleted(categoryId) mapped to toResponse()

**update():**
- Use noteRepository.findByIdNotDeleted(id) → throw ResourceNotFoundException if absent
- Ownership check → throw ForbiddenException("You do not own this note") if not owner
- Apply title and content from request
- If categoryId is not null: load and set category; if null: set category to null
- Save and return toResponse()

**delete() — SOFT DELETE, never hard delete:**
- Use noteRepository.findByIdNotDeleted(id) → throw ResourceNotFoundException if absent
- Ownership check → throw ForbiddenException if not owner
- Set note.setDeletedAt(LocalDateTime.now())
- Call noteRepository.save(note) — do NOT call noteRepository.delete()

**private NoteResponse toResponse(Note n):**
```java
return new NoteResponse(
    n.getId(),
    n.getTitle(),
    n.getContent(),
    n.getUser().getUsername(),
    n.getCategory() != null ? n.getCategory().getId() : null,
    n.getCategory() != null ? n.getCategory().getTitle() : null,
    n.getCreatedAt(),
    n.getUpdatedAt()
);
```

Verify the Category title getter name (getTitle() vs getName()) by reading Category.java first.

## Step 6 — REST Controller

`src/main/java/org/noteplus/noteplus/controller/NoteController.java`:

```java
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
    public ResponseEntity<NoteResponse> getById(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(noteService.getById(id, auth.getName()));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get notes filtered by category")
    @ApiResponse(responseCode = "200", description = "Notes retrieved")
    public ResponseEntity<List<NoteResponse>> getByCategory(@PathVariable Long categoryId) {
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
            @PathVariable Long id,
            @Valid @RequestBody UpdateNoteRequest request,
            Authentication auth) {
        return ResponseEntity.ok(noteService.update(id, request, auth.getName()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a note — must be owner or ADMIN")
    @ApiResponse(responseCode = "204", description = "Note soft-deleted successfully")
    @ApiResponse(responseCode = "403", description = "Not your note")
    @ApiResponse(responseCode = "404", description = "Note not found")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        noteService.delete(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
```

## Step 7 — Compile and smoke test

```bash
./mvnw compile
./mvnw spring-boot:run
```

Verify in Swagger (http://localhost:8080/swagger-ui/index.html):

| Test | Token role | Expected |
|------|-----------|----------|
| POST /api/notes | STUDENT token | 201 Created |
| POST /api/notes | COACH token | 201 Created |
| GET /api/notes | STUDENT token | 200 — own notes only |
| GET /api/notes | COACH token | 200 — own notes only |
| GET /api/notes/all | STUDENT token | 403 Forbidden |
| GET /api/notes/all | COACH token | 403 Forbidden |
| GET /api/notes/all | ADMIN token | 200 — all notes |
| GET /api/notes/{studentNoteId} | COACH token | 403 Forbidden |
| PUT /api/notes/{id} — other owner | STUDENT token | 403 Forbidden |
| DELETE /api/notes/{id} — own note | STUDENT token | 204 No Content |
| GET /api/notes/{id} — after delete | STUDENT token | 404 Not Found |

Do NOT proceed to note-frontend.md until all tests pass.
