package org.noteplus.noteplus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateLearningPathRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title cannot exceed 255 characters")
        String title,

        String description,

        @NotNull(message = "A student must be assigned to this learning path")
        UUID studentId,

        @NotNull(message = "A coach must be assigned to this learning path")
        UUID coachId
) {}
