package org.noteplus.noteplus.dto.response;

import java.util.UUID;

public record ReferenceResponse(
        UUID id,
        String title,
        String description,
        String url,
        FileAttachmentResponse fileAttachment
) {}
