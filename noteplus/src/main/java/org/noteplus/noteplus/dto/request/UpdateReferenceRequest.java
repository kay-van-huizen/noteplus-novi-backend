package org.noteplus.noteplus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateReferenceRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title cannot exceed 150 characters")
        String title,

        String description,

        @Size(max = 500)
        String url
) {}
