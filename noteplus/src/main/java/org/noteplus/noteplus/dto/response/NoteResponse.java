package org.noteplus.noteplus.dto.response;

import java.time.LocalDateTime;

public record NoteResponse(
        Long id,
        String title,
        String content,
        String ownerUsername,
        Long categoryId,
        String categoryTitle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
