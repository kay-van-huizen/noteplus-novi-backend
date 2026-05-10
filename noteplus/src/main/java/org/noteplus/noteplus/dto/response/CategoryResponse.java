package org.noteplus.noteplus.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String title,
        String description,
        String color,
        String status,
        UUID parentId,
        String parentTitle,
        LocalDateTime createdAt
) {}
