package org.noteplus.noteplus.service;

import org.noteplus.noteplus.dto.request.CreateLearningPathRequest;
import org.noteplus.noteplus.dto.request.UpdateLearningPathRequest;
import org.noteplus.noteplus.dto.response.LearningPathResponse;

import java.util.List;
import java.util.UUID;

public interface LearningPathService {
    LearningPathResponse create(CreateLearningPathRequest request, String username);
    LearningPathResponse getById(UUID id, String username);
    List<LearningPathResponse> getAllForUser(String username);
    List<LearningPathResponse> getAll();
    LearningPathResponse update(UUID id, UpdateLearningPathRequest request, String username);
    void delete(UUID id, String username);
    LearningPathResponse addNote(UUID learningPathId, UUID noteId, String username);
    LearningPathResponse removeNote(UUID learningPathId, UUID noteId, String username);
}
