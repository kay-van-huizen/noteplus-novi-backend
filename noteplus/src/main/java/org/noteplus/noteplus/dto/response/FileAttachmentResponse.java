package org.noteplus.noteplus.dto.response;

import java.util.UUID;

public record FileAttachmentResponse(
        UUID id,
        String fileName,
        String contentType,
        Long size
) {}
