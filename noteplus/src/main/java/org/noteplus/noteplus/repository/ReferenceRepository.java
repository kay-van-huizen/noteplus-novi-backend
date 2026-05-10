package org.noteplus.noteplus.repository;

import org.noteplus.noteplus.entity.Reference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReferenceRepository extends JpaRepository<Reference, Long> {

    @Query("SELECT r FROM Reference r JOIN r.notes n WHERE n.id = :noteId")
    List<Reference> findAllByNoteId(@Param("noteId") Long noteId);
}
