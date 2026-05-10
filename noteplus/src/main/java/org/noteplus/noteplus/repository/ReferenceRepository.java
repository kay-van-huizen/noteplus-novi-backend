package org.noteplus.noteplus.repository;

import org.noteplus.noteplus.entity.Reference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferenceRepository extends JpaRepository<Reference, UUID> {

    @Query("SELECT r FROM Reference r LEFT JOIN FETCH r.fileAttachment JOIN r.notes n WHERE n.id = :noteId")
    List<Reference> findAllByNoteId(@Param("noteId") UUID noteId);

    @Query("SELECT r FROM Reference r LEFT JOIN FETCH r.fileAttachment WHERE r.id = :id")
    Optional<Reference> findByIdWithAttachment(@Param("id") UUID id);
}
