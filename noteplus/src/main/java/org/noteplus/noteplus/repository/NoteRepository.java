package org.noteplus.noteplus.repository;

import org.noteplus.noteplus.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    @Query("SELECT n FROM Note n WHERE n.user.username = :username AND n.deletedAt IS NULL")
    List<Note> findAllByUsernameNotDeleted(String username);

    @Query("SELECT n FROM Note n WHERE n.id = :id AND n.deletedAt IS NULL")
    Optional<Note> findByIdNotDeleted(Long id);

    @Query("SELECT n FROM Note n WHERE n.deletedAt IS NULL")
    List<Note> findAllNotDeleted();

    @Query("SELECT n FROM Note n WHERE n.category.id = :categoryId AND n.deletedAt IS NULL")
    List<Note> findByCategoryIdNotDeleted(Long categoryId);
}
