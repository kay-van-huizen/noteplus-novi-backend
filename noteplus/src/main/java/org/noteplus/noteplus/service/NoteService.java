package org.noteplus.noteplus.service;

import org.noteplus.noteplus.dto.request.CreateNoteRequest;
import org.noteplus.noteplus.dto.request.UpdateNoteRequest;
import org.noteplus.noteplus.dto.response.NoteResponse;

import java.util.List;

public interface NoteService {
    NoteResponse create(CreateNoteRequest request, String username);
    NoteResponse getById(Long id, String username);
    List<NoteResponse> getAllForUser(String username);
    List<NoteResponse> getAll();
    List<NoteResponse> getByCategoryId(Long categoryId);
    NoteResponse update(Long id, UpdateNoteRequest request, String username);
    void delete(Long id, String username);
}
