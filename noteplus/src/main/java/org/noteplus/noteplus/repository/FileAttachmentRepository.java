package org.noteplus.noteplus.repository;

import org.noteplus.noteplus.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {
}
