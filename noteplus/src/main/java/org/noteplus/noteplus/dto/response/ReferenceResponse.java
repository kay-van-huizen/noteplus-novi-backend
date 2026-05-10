package org.noteplus.noteplus.dto.response;

public record ReferenceResponse(
        Long id,
        String title,
        String description,
        String url,
        FileAttachmentResponse fileAttachment
) {}
