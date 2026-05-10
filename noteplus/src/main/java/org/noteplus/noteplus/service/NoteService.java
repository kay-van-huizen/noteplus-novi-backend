package org.noteplus.noteplus.service;

import org.noteplus.noteplus.dto.request.CreateNoteRequest;
import org.noteplus.noteplus.dto.request.UpdateNoteRequest;
import org.noteplus.noteplus.dto.response.NoteResponse;

import java.util.List;
import java.util.UUID;

public interface NoteService {
    NoteResponse create(CreateNoteRequest request, String username);
    NoteResponse getById(UUID id, String username);
    List<NoteResponse> getAllForUser(String username);
    List<NoteResponse> getAll();
    List<NoteResponse> getByCategoryId(UUID categoryId);
    NoteResponse update(UUID id, UpdateNoteRequest request, String username);
    void delete(UUID id, String username);
}
