package org.noteplus.noteplus.service;

import org.noteplus.noteplus.dto.request.CreateLearningPathRequest;
import org.noteplus.noteplus.dto.request.UpdateLearningPathRequest;
import org.noteplus.noteplus.dto.response.LearningPathResponse;

import java.util.List;

public interface LearningPathService {
    LearningPathResponse create(CreateLearningPathRequest request, String username);
    LearningPathResponse getById(Long id, String username);
    List<LearningPathResponse> getAllForUser(String username);
    List<LearningPathResponse> getAll();
    LearningPathResponse update(Long id, UpdateLearningPathRequest request, String username);
    void delete(Long id, String username);
    LearningPathResponse addNote(Long learningPathId, Long noteId, String username);
    LearningPathResponse removeNote(Long learningPathId, Long noteId, String username);
}
