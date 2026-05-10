package org.noteplus.noteplus.repository;

import org.noteplus.noteplus.entity.Reference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReferenceRepository extends JpaRepository<Reference, UUID> {

    @Query("SELECT r FROM Reference r JOIN r.notes n WHERE n.id = :noteId")
    List<Reference> findAllByNoteId(@Param("noteId") UUID noteId);
}
