package org.noteplus.noteplus.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "learning_paths")
public class LearningPath extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String title;

    @Lob
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "learning_path_notes",
            joinColumns = @JoinColumn(name = "learning_path_id"),
            inverseJoinColumns = @JoinColumn(name = "note_id")
    )
    private Set<Note> notes = new LinkedHashSet<>();
}
