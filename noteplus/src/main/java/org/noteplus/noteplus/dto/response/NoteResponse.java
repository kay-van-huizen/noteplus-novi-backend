package org.noteplus.noteplus.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record NoteResponse(
        UUID id,
        String title,
        String content,
        String ownerUsername,
        UUID categoryId,
        String categoryTitle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
