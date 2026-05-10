package org.noteplus.noteplus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.noteplus.noteplus.entity.CategoryColor;
import org.noteplus.noteplus.entity.CategoryStatus;

public record UpdateCategoryRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title cannot exceed 100 characters")
        String title,

        String description,

        CategoryColor color,

        CategoryStatus status
) {}
