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

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "References", description = "Reference management nested under notes — includes file upload and download")
@SecurityRequirement(name = "bearerAuth")
public class ReferenceController {

    private final ReferenceService referenceService;

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

    @PostMapping("/api/notes/{noteId}/references/{referenceId}/attachment/delete")
    @Operation(summary = "Delete attachment via HTML form POST (browser workaround)")
    @ApiResponse(responseCode = "204", description = "File deleted, redirects to edit page")
    public ResponseEntity<Void> deleteFileFromForm(
            @PathVariable Long noteId,
            @PathVariable Long referenceId,
            Authentication auth,
            HttpServletResponse response) throws IOException {
        referenceService.deleteFile(referenceId, noteId, auth.getName());
        response.sendRedirect("/notes/" + noteId + "/edit");
        return ResponseEntity.noContent().build();
    }
}
