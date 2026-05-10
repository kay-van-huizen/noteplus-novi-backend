package org.noteplus.noteplus.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record LearningPathResponse(
        UUID id,
        String title,
        String description,
        String studentUsername,
        String coachUsername,
        List<NoteResponse> notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
