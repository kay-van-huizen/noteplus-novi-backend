package org.noteplus.noteplus.dto.response;

public record FileAttachmentResponse(
        Long id,
        String fileName,
        String contentType,
        Long size
) {}
