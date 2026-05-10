package org.noteplus.noteplus.service.impl;

import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.CreateNoteRequest;
import org.noteplus.noteplus.dto.request.UpdateNoteRequest;
import org.noteplus.noteplus.dto.response.NoteResponse;
import org.noteplus.noteplus.entity.Note;
import org.noteplus.noteplus.exception.ForbiddenException;
import org.noteplus.noteplus.exception.ResourceNotFoundException;
import org.noteplus.noteplus.repository.CategoryRepository;
import org.noteplus.noteplus.repository.NoteRepository;
import org.noteplus.noteplus.repository.UserRepository;
import org.noteplus.noteplus.service.NoteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public NoteResponse create(CreateNoteRequest request, String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var note = new Note();
        note.setTitle(request.title());
        note.setContent(request.content());
        note.setUser(user);

        if (request.categoryId() != null) {
            var category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId()));
            note.setCategory(category);
        }

        return toResponse(noteRepository.save(note));
    }

    @Override
    @Transactional(readOnly = true)
    public NoteResponse getById(UUID id, String username) {
        var note = noteRepository.findByIdNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found: " + id));

        if (!note.getUser().getUsername().equals(username) && !isAdmin(username)) {
            throw new ForbiddenException("You do not have access to this note");
        }

        return toResponse(note);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteResponse> getAllForUser(String username) {
        return noteRepository.findAllByUsernameNotDeleted(username).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteResponse> getAll() {
        return noteRepository.findAllNotDeleted().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteResponse> getByCategoryId(UUID categoryId) {
        return noteRepository.findByCategoryIdNotDeleted(categoryId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public NoteResponse update(UUID id, UpdateNoteRequest request, String username) {
        var note = noteRepository.findByIdNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found: " + id));

        if (!note.getUser().getUsername().equals(username) && !isAdmin(username)) {
            throw new ForbiddenException("You do not own this note");
        }

        note.setTitle(request.title());
        note.setContent(request.content());

        if (request.categoryId() != null) {
            var category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId()));
            note.setCategory(category);
        } else {
            note.setCategory(null);
        }

        return toResponse(noteRepository.save(note));
    }

    @Override
    @Transactional
    public void delete(UUID id, String username) {
        var note = noteRepository.findByIdNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found: " + id));

        if (!note.getUser().getUsername().equals(username) && !isAdmin(username)) {
            throw new ForbiddenException("You do not own this note");
        }

        note.setDeletedAt(LocalDateTime.now());
        noteRepository.save(note);
    }

    private boolean isAdmin(String username) {
        return userRepository.findByUsername(username)
                .map(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN")))
                .orElse(false);
    }

    private NoteResponse toResponse(Note n) {
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
    }
}
