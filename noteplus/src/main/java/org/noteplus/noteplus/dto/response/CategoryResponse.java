package org.noteplus.noteplus.dto.response;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String title,
        String description,
        String color,
        String status,
        Long parentId,
        String parentTitle,
        LocalDateTime createdAt
) {}
