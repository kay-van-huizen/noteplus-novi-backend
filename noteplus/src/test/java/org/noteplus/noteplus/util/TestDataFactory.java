package org.noteplus.noteplus.util;

import org.noteplus.noteplus.entity.Category;
import org.noteplus.noteplus.entity.Note;
import org.noteplus.noteplus.entity.Role;
import org.noteplus.noteplus.entity.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Shared factory for test data — all tests must use this class instead of
 * creating entities inline, so test data stays consistent and changes propagate.
 */
public class TestDataFactory {

    public static Role createStudentRole() {
        Role role = new Role();
        role.setName("ROLE_STUDENT");
        return role;
    }

    public static Role createAdminRole() {
        Role role = new Role();
        role.setName("ROLE_ADMIN");
        return role;
    }

    public static User createStudent() {
        User user = new User();
        user.setName("Student One");
        user.setUsername("student1");
        user.setEmail("student@test.nl");
        user.setPassword("$2a$10$slYQmyNdgTY18LGvgxLJLuqSTTaVKFKzLHjXnAP5RQyb6MBtFm.Dm");
        user.setRoles(Set.of(createStudentRole()));
        return user;
    }

    public static User createAdmin() {
        User user = new User();
        user.setName("Admin User");
        user.setUsername("admin");
        user.setEmail("admin@test.nl");
        user.setPassword("$2a$10$slYQmyNdgTY18LGvgxLJLuqSTTaVKFKzLHjXnAP5RQyb6MBtFm.Dm");
        user.setRoles(Set.of(createAdminRole()));
        return user;
    }

    public static User createCoach() {
        User user = new User();
        user.setName("Coach One");
        user.setUsername("coach1");
        user.setEmail("coach@test.nl");
        user.setPassword("$2a$10$slYQmyNdgTY18LGvgxLJLuqSTTaVKFKzLHjXnAP5RQyb6MBtFm.Dm");
        user.setRoles(Set.of(createCoachRole()));
        return user;
    }

    public static Role createCoachRole() {
        Role role = new Role();
        role.setName("ROLE_COACH");
        return role;
    }

    // ── Categories ────────────────────────────────────────────────────────

    public static Category createCategory() {
        Category cat = new Category();
        cat.setId(10L);
        cat.setTitle("Test Category");
        return cat;
    }

    // ── Notes ─────────────────────────────────────────────────────────────

    public static Note createNote(User owner) {
        Note note = new Note();
        note.setId(100L);
        note.setTitle("Test Note Title");
        note.setContent("Test note content body");
        note.setUser(owner);
        note.setDeletedAt(null);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        return note;
    }

    public static Note createNoteWithCategory(User owner, Category category) {
        Note note = createNote(owner);
        note.setCategory(category);
        return note;
    }

    public static Note createSoftDeletedNote(User owner) {
        Note note = createNote(owner);
        note.setDeletedAt(LocalDateTime.now().minusHours(1));
        return note;
    }

    /** Creates a Spring Security UserDetails with ROLE_STUDENT authority. */
    public static UserDetails createStudentUserDetails() {
        return org.springframework.security.core.userdetails.User.builder()
                .username("student1")
                .password("$2a$10$hashed")
                .authorities("ROLE_STUDENT")
                .build();
    }

    /** Creates a Spring Security UserDetails with ROLE_STUDENT for a given username. */
    public static UserDetails createUserDetailsWith(String username) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password("$2a$10$hashed")
                .authorities("ROLE_STUDENT")
                .build();
    }
}
