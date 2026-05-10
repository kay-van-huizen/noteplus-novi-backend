package org.noteplus.noteplus.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank
        @Email(message = "Please enter a valid email address")
        String email
) {}
