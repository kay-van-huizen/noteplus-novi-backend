package org.noteplus.noteplus.repository;

import org.noteplus.noteplus.entity.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {

    @Query("SELECT lp FROM LearningPath lp WHERE lp.student.username = :username OR lp.coach.username = :username")
    List<LearningPath> findAllByUsername(@Param("username") String username);

    List<LearningPath> findByStudentUsername(String username);

    List<LearningPath> findByCoachUsername(String username);
}
