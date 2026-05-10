package org.noteplus.noteplus.service.impl;

import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.CreateLearningPathRequest;
import org.noteplus.noteplus.dto.request.UpdateLearningPathRequest;
import org.noteplus.noteplus.dto.response.LearningPathResponse;
import org.noteplus.noteplus.dto.response.NoteResponse;
import org.noteplus.noteplus.entity.LearningPath;
import org.noteplus.noteplus.entity.Note;
import org.noteplus.noteplus.exception.ForbiddenException;
import org.noteplus.noteplus.exception.ResourceNotFoundException;
import org.noteplus.noteplus.repository.LearningPathRepository;
import org.noteplus.noteplus.repository.NoteRepository;
import org.noteplus.noteplus.repository.UserRepository;
import org.noteplus.noteplus.service.LearningPathService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningPathServiceImpl implements LearningPathService {

    private final LearningPathRepository learningPathRepository;
    private final UserRepository userRepository;
    private final NoteRepository noteRepository;

    @Override
    @Transactional
    public LearningPathResponse create(CreateLearningPathRequest request, String username) {
        userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var student = userRepository.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        var coach = userRepository.findById(request.coachId())
                .orElseThrow(() -> new ResourceNotFoundException("Coach not found"));

        var lp = new LearningPath();
        lp.setTitle(request.title());
        lp.setDescription(request.description());
        lp.setStudent(student);
        lp.setCoach(coach);

        return toResponse(learningPathRepository.save(lp));
    }

    @Override
    @Transactional(readOnly = true)
    public LearningPathResponse getById(UUID id, String username) {
        var lp = findOrThrow(id);
        checkAccess(lp, username);
        return toResponse(lp);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningPathResponse> getAllForUser(String username) {
        return learningPathRepository.findAllByUsername(username).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningPathResponse> getAll() {
        return learningPathRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public LearningPathResponse update(UUID id, UpdateLearningPathRequest request, String username) {
        var lp = findOrThrow(id);
        checkAccess(lp, username);

        lp.setTitle(request.title());
        lp.setDescription(request.description());

        return toResponse(learningPathRepository.save(lp));
    }

    @Override
    @Transactional
    public void delete(UUID id, String username) {
        var lp = findOrThrow(id);

        boolean isAdmin = isAdmin(username);
        boolean isCoach = lp.getCoach().getUsername().equals(username);

        if (!isAdmin && !isCoach) {
            throw new ForbiddenException("Only the coach or admin can delete a learning path");
        }

        learningPathRepository.delete(lp);
    }

    @Override
    @Transactional
    public LearningPathResponse addNote(UUID learningPathId, UUID noteId, String username) {
        var lp = findOrThrow(learningPathId);
        checkAccess(lp, username);

        var note = noteRepository.findByIdNotDeleted(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        boolean alreadyLinked = lp.getNotes().stream().anyMatch(n -> n.getId().equals(noteId));
        if (!alreadyLinked) {
            lp.getNotes().add(note);
            learningPathRepository.save(lp);
        }

        return toResponse(lp);
    }

    @Override
    @Transactional
    public LearningPathResponse removeNote(UUID learningPathId, UUID noteId, String username) {
        var lp = findOrThrow(learningPathId);
        checkAccess(lp, username);

        boolean removed = lp.getNotes().removeIf(n -> n.getId().equals(noteId));
        if (!removed) {
            throw new ResourceNotFoundException("Note not in this learning path");
        }

        return toResponse(learningPathRepository.save(lp));
    }

    private LearningPath findOrThrow(UUID id) {
        return learningPathRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Learning path not found: " + id));
    }

    private void checkAccess(LearningPath lp, String username) {
        if (isAdmin(username)) return;
        boolean isStudent = lp.getStudent().getUsername().equals(username);
        boolean isCoach = lp.getCoach().getUsername().equals(username);
        if (!isStudent && !isCoach) {
            throw new ForbiddenException("You do not have access to this learning path");
        }
    }

    private boolean isAdmin(String username) {
        return userRepository.findByUsername(username)
                .map(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN")))
                .orElse(false);
    }

    private LearningPathResponse toResponse(LearningPath lp) {
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
    }

    private NoteResponse noteToResponse(Note n) {
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
