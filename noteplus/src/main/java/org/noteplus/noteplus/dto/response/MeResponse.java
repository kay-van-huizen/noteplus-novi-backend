package org.noteplus.noteplus.dto.response;

import java.util.List;
import java.util.UUID;

public record MeResponse(UUID id, String name, String username, List<String> roles) {}
