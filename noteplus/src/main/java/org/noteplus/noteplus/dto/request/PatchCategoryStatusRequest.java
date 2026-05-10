package org.noteplus.noteplus.dto.request;

import jakarta.validation.constraints.NotNull;
import org.noteplus.noteplus.entity.CategoryStatus;

public record PatchCategoryStatusRequest(
        @NotNull(message = "Status is required")
        CategoryStatus status
) {}
