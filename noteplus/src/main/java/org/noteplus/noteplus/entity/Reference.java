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
@Table(name = "reference_items")
public class Reference extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String title;

    @Lob
    private String description;

    @Column(length = 500)
    private String url;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "file_attachment_id", unique = true)
    private FileAttachment fileAttachment;

    @ManyToMany(mappedBy = "references", fetch = FetchType.LAZY)
    private Set<Note> notes = new LinkedHashSet<>();
}
