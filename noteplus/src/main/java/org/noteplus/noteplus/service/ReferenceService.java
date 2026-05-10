package org.noteplus.noteplus.service;

import org.noteplus.noteplus.dto.request.CreateReferenceRequest;
import org.noteplus.noteplus.dto.request.UpdateReferenceRequest;
import org.noteplus.noteplus.dto.response.ReferenceResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ReferenceService {
    List<ReferenceResponse> getAllForNote(UUID noteId, String username);
    ReferenceResponse create(UUID noteId, CreateReferenceRequest request, String username);
    ReferenceResponse update(UUID referenceId, UUID noteId, UpdateReferenceRequest request, String username);
    void delete(UUID referenceId, UUID noteId, String username);

    ReferenceResponse uploadFile(UUID referenceId, UUID noteId, MultipartFile file, String username);
    Resource downloadFile(UUID referenceId, UUID noteId, String username);
    void deleteFile(UUID referenceId, UUID noteId, String username);
}
