package org.noteplus.noteplus.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notes")
public class Note extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String title;

    @Lob
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "note_references",
            joinColumns = @JoinColumn(name = "note_id"),
            inverseJoinColumns = @JoinColumn(name = "reference_id")
    )
    private Set<Reference> references = new LinkedHashSet<>();

    @ManyToMany(mappedBy = "notes", fetch = FetchType.LAZY)
    private Set<LearningPath> learningPaths = new LinkedHashSet<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
