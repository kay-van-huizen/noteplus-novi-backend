package org.noteplus.noteplus.service.impl;

import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.CreateReferenceRequest;
import org.noteplus.noteplus.dto.request.UpdateReferenceRequest;
import org.noteplus.noteplus.dto.response.FileAttachmentResponse;
import org.noteplus.noteplus.dto.response.ReferenceResponse;
import org.noteplus.noteplus.entity.FileAttachment;
import org.noteplus.noteplus.entity.Note;
import org.noteplus.noteplus.entity.Reference;
import org.noteplus.noteplus.exception.ForbiddenException;
import org.noteplus.noteplus.exception.ResourceNotFoundException;
import org.noteplus.noteplus.repository.NoteRepository;
import org.noteplus.noteplus.repository.ReferenceRepository;
import org.noteplus.noteplus.service.FileStorageService;
import org.noteplus.noteplus.service.ReferenceService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReferenceServiceImpl implements ReferenceService {

    private final ReferenceRepository referenceRepository;
    private final NoteRepository noteRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<ReferenceResponse> getAllForNote(UUID noteId, String username) {
        loadNoteForUser(noteId, username);
        return referenceRepository.findAllByNoteId(noteId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReferenceResponse create(UUID noteId, CreateReferenceRequest request, String username) {
        var note = loadNoteForUser(noteId, username);

        var reference = new Reference();
        reference.setTitle(request.title());
        reference.setDescription(request.description());
        reference.setUrl(request.url());

        referenceRepository.save(reference);
        note.getReferences().add(reference);
        noteRepository.save(note);

        return toResponse(reference);
    }

    @Override
    @Transactional
    public ReferenceResponse update(UUID referenceId, UUID noteId, UpdateReferenceRequest request, String username) {
        loadNoteForUser(noteId, username);
        var reference = findReferenceForNote(referenceId, noteId);

        reference.setTitle(request.title());
        reference.setDescription(request.description());
        reference.setUrl(request.url());

        return toResponse(referenceRepository.save(reference));
    }

    @Override
    @Transactional
    public void delete(UUID referenceId, UUID noteId, String username) {
        var note = loadNoteForUser(noteId, username);
        var reference = findReferenceForNote(referenceId, noteId);

        note.getReferences().remove(reference);
        noteRepository.save(note);
        referenceRepository.delete(reference);
    }

    @Override
    @Transactional
    public ReferenceResponse uploadFile(UUID referenceId, UUID noteId, MultipartFile file, String username) {
        loadNoteForUser(noteId, username);
        var reference = findReferenceForNote(referenceId, noteId);

        if (reference.getFileAttachment() != null) {
            fileStorageService.delete(reference.getFileAttachment().getStoragePath());
        }

        String storagePath = fileStorageService.store(file);

        var fa = new FileAttachment();
        fa.setFileName(file.getOriginalFilename());
        fa.setStoragePath(storagePath);
        fa.setContentType(file.getContentType());
        fa.setSize(file.getSize());

        reference.setFileAttachment(fa);
        return toResponse(referenceRepository.save(reference));
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadFile(UUID referenceId, UUID noteId, String username) {
        loadNoteForUser(noteId, username);
        var reference = findReferenceForNote(referenceId, noteId);

        if (reference.getFileAttachment() == null) {
            throw new ResourceNotFoundException("No file attached to this reference");
        }

        return fileStorageService.load(reference.getFileAttachment().getStoragePath());
    }

    @Override
    @Transactional
    public void deleteFile(UUID referenceId, UUID noteId, String username) {
        loadNoteForUser(noteId, username);
        var reference = findReferenceForNote(referenceId, noteId);

        if (reference.getFileAttachment() == null) {
            throw new ResourceNotFoundException("No file attached to this reference");
        }

        fileStorageService.delete(reference.getFileAttachment().getStoragePath());
        reference.setFileAttachment(null);
        referenceRepository.save(reference);
    }

    private Note loadNoteForUser(UUID noteId, String username) {
        var note = noteRepository.findByIdNotDeleted(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found: " + noteId));
        if (!note.getUser().getUsername().equals(username)) {
            throw new ForbiddenException("You do not have access to this note");
        }
        return note;
    }

    private Reference findReferenceForNote(UUID referenceId, UUID noteId) {
        var reference = referenceRepository.findByIdWithAttachment(referenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Reference not found: " + referenceId));
        boolean belongsToNote = referenceRepository.findAllByNoteId(noteId).stream()
                .anyMatch(r -> r.getId().equals(referenceId));
        if (!belongsToNote) {
            throw new ForbiddenException("This reference does not belong to this note");
        }
        return reference;
    }

    private ReferenceResponse toResponse(Reference r) {
        FileAttachmentResponse attachmentResponse = null;
        if (r.getFileAttachment() != null) {
            var fa = r.getFileAttachment();
            attachmentResponse = new FileAttachmentResponse(
                    fa.getId(),
                    fa.getFileName(),
                    fa.getContentType(),
                    fa.getSize()
            );
        }
        return new ReferenceResponse(r.getId(), r.getTitle(), r.getDescription(), r.getUrl(), attachmentResponse);
    }
}
