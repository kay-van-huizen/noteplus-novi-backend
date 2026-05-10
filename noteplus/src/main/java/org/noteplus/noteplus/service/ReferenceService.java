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

    ReferenceResponse uploadFile(Long referenceId, Long noteId, MultipartFile file, String username);
    Resource downloadFile(Long referenceId, Long noteId, String username);
    void deleteFile(Long referenceId, Long noteId, String username);
}
