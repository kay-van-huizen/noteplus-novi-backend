package org.noteplus.noteplus.dto.response;

import java.util.UUID;

public record UserResponse(UUID id, String username, String email) {}
