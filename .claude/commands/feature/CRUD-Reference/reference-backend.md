---
description: Builds the Reference backend — CRUD nested under notes, FileStorageService, and file upload/download endpoints for FileAttachment. This covers two school requirements at once: Reference CRUD and the mandatory file upload/download feature.
allowed-tools: Read, Write, Bash(git checkout:*), Bash(git pull:*), Bash(./mvnw compile:*), Glob, Grep
---

# Reference + File Upload Backend — NotePlus

## What this feature covers

- Reference CRUD nested under a Note (`/api/notes/{noteId}/references`)
- One reference can have one FileAttachment (one-to-one, cascade ALL + orphan removal)
- File upload: `POST /api/references/{referenceId}/attachment`
- File download: `GET /api/references/{referenceId}/attachment`
- File delete: `DELETE /api/references/{referenceId}/attachment`

This satisfies the mandatory school requirement: "the web-API must allow files to be uploaded and downloaded."

## Role rules

Ownership flows through the Note. If you own the Note, you own its References.

| Action | ADMIN | COACH | STUDENT |
|--------|-------|-------|---------|
| GET references for a note | ✅ | ✅ (own note) | ✅ (own note) |
| POST add reference to note | ✅ | ✅ (own note) | ✅ (own note) |
| PUT update reference | ✅ | ✅ (own note) | ✅ (own note) |
| DELETE reference | ✅ | ✅ (own note) | ✅ (own note) |
| POST upload file to reference | ✅ | ✅ (own note) | ✅ (own note) |
| GET download file | ✅ | ✅ (own note) | ✅ (own note) |
| DELETE file from reference | ✅ | ✅ (own note) | ✅ (own note) |

## Step 0 — Read context first (MANDATORY)

Before writing a single line of code, read ALL of these:

```
src/main/java/org/noteplus/noteplus/entity/Reference.java         ← already exists, read carefully
src/main/java/org/noteplus/noteplus/entity/FileAttachment.java    ← read all fields
src/main/java/org/noteplus/noteplus/entity/Note.java              ← check M:M owning side
src/main/java/org/noteplus/noteplus/entity/BaseEntity.java        ← id type (Long IDENTITY)
src/main/java/org/noteplus/noteplus/entity/User.java
src/main/java/org/noteplus/noteplus/repository/NoteRepository.java
src/main/java/org/noteplus/noteplus/service/impl/NoteServiceImpl.java
src/main/java/org/noteplus/noteplus/controller/NoteController.java
src/main/java/org/noteplus/noteplus/exception/ForbiddenException.java
src/main/java/org/noteplus/noteplus/exception/ResourceNotFoundException.java
src/main/resources/application.properties
CLAUDE.md
```

## Step 2 — Verify Note entity has the M:M owning side

Read `Note.java`. It MUST contain a `references` field like this:

```java
@ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
@JoinTable(
    name = "note_reference_items",
    joinColumns = @JoinColumn(name = "note_id"),
    inverseJoinColumns = @JoinColumn(name = "reference_id")
)
private Set<Reference> references = new LinkedHashSet<>();
```

If this field is ABSENT from Note.java, add it now.
Do NOT add CascadeType.REMOVE here — deleting a Note should not auto-delete its References,
because a Reference could theoretically be shared. The Reference is deleted explicitly.

## Step 3 — Verify application.properties for file upload

Add these lines to `application.properties` if they are not already present:

```properties
# File upload settings
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Directory where uploaded files are stored
file.upload-dir=uploads
```

The `uploads/` directory must already be in `.gitignore` — verify this.

## Step 4 — Repository

Create `src/main/java/org/noteplus/noteplus/repository/ReferenceRepository.java`:

```java
package org.noteplus.noteplus.repository;

import org.noteplus.noteplus.entity.Reference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReferenceRepository extends JpaRepository<Reference, Long> {

    // All references belonging to a specific note
    @Query("SELECT r FROM Reference r JOIN r.notes n WHERE n.id = :noteId")
    List<Reference> findAllByNoteId(Long noteId);
}
```

Also create `src/main/java/org/noteplus/noteplus/repository/FileAttachmentRepository.java`:

```java
package org.noteplus.noteplus.repository;

import org.noteplus.noteplus.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {
}
```

## Step 5 — DTOs

Read Reference.java and FileAttachment.java for exact field names before writing these.

`src/main/java/org/noteplus/noteplus/dto/request/CreateReferenceRequest.java`:
```java
package org.noteplus.noteplus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReferenceRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title cannot exceed 150 characters")
    String title,

    String description,   // nullable — free text

    @Size(max = 500, message = "URL cannot exceed 500 characters")
    String url            // nullable — external link
) {}
```

`src/main/java/org/noteplus/noteplus/dto/request/UpdateReferenceRequest.java`:
```java
package org.noteplus.noteplus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateReferenceRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title cannot exceed 150 characters")
    String title,

    String description,

    @Size(max = 500)
    String url
) {}
```

`src/main/java/org/noteplus/noteplus/dto/response/FileAttachmentResponse.java`:
```java
package org.noteplus.noteplus.dto.response;

public record FileAttachmentResponse(
    Long id,
    String originalFilename,
    String contentType,
    Long fileSize
) {}
```

Read FileAttachment.java to confirm these field names match the entity before using them.

`src/main/java/org/noteplus/noteplus/dto/response/ReferenceResponse.java`:
```java
package org.noteplus.noteplus.dto.response;

public record ReferenceResponse(
    Long id,
    String title,
    String description,
    String url,
    FileAttachmentResponse fileAttachment   // null if no file uploaded
) {}
```

## Step 6 — FileStorageService

`src/main/java/org/noteplus/noteplus/service/FileStorageService.java`:
```java
package org.noteplus.noteplus.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file);
    Resource load(String storedFilename);
    void delete(String storedFilename);
}
```

`src/main/java/org/noteplus/noteplus/service/impl/FileStorageServiceImpl.java`:

- `@Service`
- Inject upload directory path from `@Value("${file.upload-dir}")`
- Use `@PostConstruct` to create the directory on startup

```java
package org.noteplus.noteplus.service.impl;

import jakarta.annotation.PostConstruct;
import org.noteplus.noteplus.exception.ResourceNotFoundException;
import org.noteplus.noteplus.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(uploadDir);
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadDir, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty file");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new IllegalArgumentException("Invalid filename: " + originalFilename);
        }
        // Prefix with UUID to avoid collisions and prevent filename enumeration
        String storedFilename = UUID.randomUUID() + "_" + originalFilename;
        try {
            Files.copy(file.getInputStream(), rootLocation.resolve(storedFilename),
                StandardCopyOption.REPLACE_EXISTING);
            return storedFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + storedFilename, e);
        }
    }

    @Override
    public Resource load(String storedFilename) {
        try {
            Path filePath = rootLocation.resolve(storedFilename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new ResourceNotFoundException("File not found: " + storedFilename);
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File not found: " + storedFilename);
        }
    }

    @Override
    public void delete(String storedFilename) {
        try {
            Path filePath = rootLocation.resolve(storedFilename).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + storedFilename, e);
        }
    }
}
```

## Step 7 — Service interface

`src/main/java/org/noteplus/noteplus/service/ReferenceService.java`:

```java
package org.noteplus.noteplus.service;

import org.noteplus.noteplus.dto.request.CreateReferenceRequest;
import org.noteplus.noteplus.dto.request.UpdateReferenceRequest;
import org.noteplus.noteplus.dto.response.ReferenceResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReferenceService {
    List<ReferenceResponse> getAllForNote(Long noteId, String username);
    ReferenceResponse create(Long noteId, CreateReferenceRequest request, String username);
    ReferenceResponse update(Long referenceId, Long noteId, UpdateReferenceRequest request, String username);
    void delete(Long referenceId, Long noteId, String username);

    // File operations
    ReferenceResponse uploadFile(Long referenceId, Long noteId, MultipartFile file, String username);
    Resource downloadFile(Long referenceId, Long noteId, String username);
    void deleteFile(Long referenceId, Long noteId, String username);
}
```

## Step 8 — Service implementation

`src/main/java/org/noteplus/noteplus/service/impl/ReferenceServiceImpl.java`:

- `@Service @RequiredArgsConstructor`
- Inject: `ReferenceRepository`, `NoteRepository`, `FileAttachmentRepository`, `FileStorageService`

**Ownership check helper — use this in EVERY method:**
```java
private Note loadNoteForUser(Long noteId, String username) {
    Note note = noteRepository.findByIdNotDeleted(noteId)
        .orElseThrow(() -> new ResourceNotFoundException("Note not found: " + noteId));
    if (!note.getUser().getUsername().equals(username)) {
        throw new ForbiddenException("You do not have access to this note");
    }
    return note;
}
```

**getAllForNote():**
- Call loadNoteForUser(noteId, username) — throws if no access
- Return referenceRepository.findAllByNoteId(noteId) mapped to toResponse()

**create():**
- Call loadNoteForUser(noteId, username)
- Build Reference — set title, description, url
- Save reference via referenceRepository.save(reference)
- Add to note's references set: note.getReferences().add(reference)
- Save note via noteRepository.save(note)
- Return toResponse(reference)

**update():**
- Call loadNoteForUser(noteId, username) — verify note access
- Load reference by referenceId → throw ResourceNotFoundException if absent
- Verify reference belongs to this note:
  if referenceRepository.findAllByNoteId(noteId).stream().noneMatch(r -> r.getId().equals(referenceId))
  → throw ForbiddenException("This reference does not belong to this note")
- Apply title, description, url
- Save and return toResponse()

**delete():**
- Call loadNoteForUser(noteId, username)
- Load reference → throw ResourceNotFoundException if absent
- Verify reference belongs to note (same check as update)
- Remove from note's references set: note.getReferences().remove(reference)
- Save note, then referenceRepository.delete(reference)
- FileAttachment is deleted automatically via orphan removal + cascade ALL

**uploadFile():**
- Call loadNoteForUser(noteId, username)
- Load reference → throw ResourceNotFoundException if absent
- Verify reference belongs to note
- If reference already has a FileAttachment:
  → delete the old physical file via fileStorageService.delete(existing.getStoredFilename())
  → the old FileAttachment entity will be replaced (orphan removal handles cleanup)
- Store the new file: String storedFilename = fileStorageService.store(file)
- Read FileAttachment.java to know its exact fields, then build and populate it:
  set originalFilename, storedFilename, contentType, fileSize
- reference.setFileAttachment(fileAttachment)
- Save reference and return toResponse()

**downloadFile():**
- Call loadNoteForUser(noteId, username)
- Load reference → throw ResourceNotFoundException if absent
- Verify reference belongs to note
- If reference.getFileAttachment() == null → throw ResourceNotFoundException("No file attached to this reference")
- Return fileStorageService.load(reference.getFileAttachment().getStoredFilename())

**deleteFile():**
- Call loadNoteForUser(noteId, username)
- Load reference → throw ResourceNotFoundException if absent
- Verify reference belongs to note
- If reference.getFileAttachment() == null → throw ResourceNotFoundException("No file attached")
- fileStorageService.delete(reference.getFileAttachment().getStoredFilename())
- reference.setFileAttachment(null)  ← orphan removal deletes the entity
- Save reference

**private ReferenceResponse toResponse(Reference r):**
```java
FileAttachmentResponse attachmentResponse = null;
if (r.getFileAttachment() != null) {
    var fa = r.getFileAttachment();
    // Replace field names below with actual FileAttachment entity field names
    attachmentResponse = new FileAttachmentResponse(
        fa.getId(),
        fa.getOriginalFilename(),
        fa.getContentType(),
        fa.getFileSize()
    );
}
return new ReferenceResponse(r.getId(), r.getTitle(), r.getDescription(), r.getUrl(), attachmentResponse);
```

Read FileAttachment.java to confirm the exact getter names before writing toResponse().

## Step 9 — REST Controller

`src/main/java/org/noteplus/noteplus/controller/ReferenceController.java`:

```java
package org.noteplus.noteplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.CreateReferenceRequest;
import org.noteplus.noteplus.dto.request.UpdateReferenceRequest;
import org.noteplus.noteplus.dto.response.ReferenceResponse;
import org.noteplus.noteplus.service.ReferenceService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "References", description = "Reference management nested under notes — includes file upload and download")
@SecurityRequirement(name = "bearerAuth")
public class ReferenceController {

    private final ReferenceService referenceService;

    // --- Reference CRUD ---

    @GetMapping("/api/notes/{noteId}/references")
    @Operation(summary = "Get all references for a note")
    @ApiResponse(responseCode = "200", description = "References retrieved")
    @ApiResponse(responseCode = "403", description = "Not your note")
    @ApiResponse(responseCode = "404", description = "Note not found")
    public ResponseEntity<List<ReferenceResponse>> getAllForNote(
            @PathVariable Long noteId,
            Authentication auth) {
        return ResponseEntity.ok(referenceService.getAllForNote(noteId, auth.getName()));
    }

    @PostMapping("/api/notes/{noteId}/references")
    @Operation(summary = "Add a reference to a note")
    @ApiResponse(responseCode = "201", description = "Reference created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "403", description = "Not your note")
    @ApiResponse(responseCode = "404", description = "Note not found")
    public ResponseEntity<ReferenceResponse> create(
            @PathVariable Long noteId,
            @Valid @RequestBody CreateReferenceRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(referenceService.create(noteId, request, auth.getName()));
    }

    @PutMapping("/api/notes/{noteId}/references/{referenceId}")
    @Operation(summary = "Update a reference")
    @ApiResponse(responseCode = "200", description = "Reference updated")
    @ApiResponse(responseCode = "403", description = "Not your note or reference")
    @ApiResponse(responseCode = "404", description = "Note or reference not found")
    public ResponseEntity<ReferenceResponse> update(
            @PathVariable Long noteId,
            @PathVariable Long referenceId,
            @Valid @RequestBody UpdateReferenceRequest request,
            Authentication auth) {
        return ResponseEntity.ok(referenceService.update(referenceId, noteId, request, auth.getName()));
    }

    @DeleteMapping("/api/notes/{noteId}/references/{referenceId}")
    @Operation(summary = "Delete a reference from a note")
    @ApiResponse(responseCode = "204", description = "Reference deleted")
    @ApiResponse(responseCode = "403", description = "Not your note or reference")
    @ApiResponse(responseCode = "404", description = "Note or reference not found")
    public ResponseEntity<Void> delete(
            @PathVariable Long noteId,
            @PathVariable Long referenceId,
            Authentication auth) {
        referenceService.delete(referenceId, noteId, auth.getName());
        return ResponseEntity.noContent().build();
    }

    // --- File Upload / Download ---

    @PostMapping(value = "/api/notes/{noteId}/references/{referenceId}/attachment",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file to a reference (PDF, Word, image, certificate, etc.)")
    @ApiResponse(responseCode = "200", description = "File uploaded and attached to reference")
    @ApiResponse(responseCode = "400", description = "File is empty or invalid")
    @ApiResponse(responseCode = "403", description = "Not your note")
    @ApiResponse(responseCode = "404", description = "Note or reference not found")
    public ResponseEntity<ReferenceResponse> uploadFile(
            @PathVariable Long noteId,
            @PathVariable Long referenceId,
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        return ResponseEntity.ok(
                referenceService.uploadFile(referenceId, noteId, file, auth.getName()));
    }

    @GetMapping("/api/notes/{noteId}/references/{referenceId}/attachment")
    @Operation(summary = "Download the file attached to a reference")
    @ApiResponse(responseCode = "200", description = "File downloaded")
    @ApiResponse(responseCode = "404", description = "No file attached or reference not found")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long noteId,
            @PathVariable Long referenceId,
            Authentication auth) {
        Resource file = referenceService.downloadFile(referenceId, noteId, auth.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    @DeleteMapping("/api/notes/{noteId}/references/{referenceId}/attachment")
    @Operation(summary = "Delete the file attached to a reference")
    @ApiResponse(responseCode = "204", description = "File deleted")
    @ApiResponse(responseCode = "404", description = "No file attached or reference not found")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long noteId,
            @PathVariable Long referenceId,
            Authentication auth) {
        referenceService.deleteFile(referenceId, noteId, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
```

## Step 10 — Smoke test

Verify in Swagger:

| Test | Token | Expected |
|------|-------|----------|
| POST /api/notes/{noteId}/references | STUDENT (own note) | 201 Created |
| POST /api/notes/{noteId}/references | COACH (other's note) | 403 Forbidden |
| GET /api/notes/{noteId}/references | STUDENT (own note) | 200 with list |
| PUT /api/notes/{noteId}/references/{id} | STUDENT (own note) | 200 Updated |
| DELETE /api/notes/{noteId}/references/{id} | STUDENT (own note) | 204 No Content |
| POST /api/notes/{noteId}/references/{id}/attachment (PDF file) | STUDENT | 200 with attachment |
| GET /api/notes/{noteId}/references/{id}/attachment | STUDENT | 200 file download |
| DELETE /api/notes/{noteId}/references/{id}/attachment | STUDENT | 204 No Content |
| GET /api/notes/{noteId}/references/{id}/attachment after delete | STUDENT | 404 Not Found |

Do NOT proceed to reference-frontend.md until all tests pass.
