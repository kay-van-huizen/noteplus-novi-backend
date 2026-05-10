package org.noteplus.noteplus.repository;

import org.noteplus.noteplus.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment, UUID> {
}
