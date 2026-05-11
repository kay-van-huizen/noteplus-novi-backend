package org.noteplus.noteplus.util;

import org.noteplus.noteplus.entity.Category;
import org.noteplus.noteplus.entity.Note;
import org.noteplus.noteplus.entity.Role;
import org.noteplus.noteplus.entity.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class TestDataFactory {

    private TestDataFactory() {}

    public static final UUID NOTE_ID     = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID CATEGORY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID NOT_FOUND_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    public static Role createStudentRole() {
        Role role = new Role();
        role.setName("ROLE_STUDENT");
        return role;
    }

    public static Role createCoachRole() {
        Role role = new Role();
        role.setName("ROLE_COACH");
        return role;
    }

    public static User createStudent() {
        User user = new User();
        user.setName("Student One");
        user.setUsername("student1");
        user.setEmail("student1@test.com");
        user.setPassword("encodedPassword");
        user.setRoles(Set.of(createStudentRole()));
        return user;
    }

    public static User createCoach() {
        User user = new User();
        user.setName("Coach One");
        user.setUsername("coach1");
        user.setEmail("coach1@test.com");
        user.setPassword("encodedPassword");
        user.setRoles(Set.of(createCoachRole()));
        return user;
    }

    public static UserDetails createStudentUserDetails() {
        return org.springframework.security.core.userdetails.User.builder()
                .username("student1")
                .password("encodedPassword")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .build();
    }

    public static UserDetails createUserDetailsWith(String username) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password("encodedPassword")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .build();
    }

    public static Note createNote(User owner) {
        Note note = new Note();
        note.setId(NOTE_ID);
        note.setTitle("Test Note Title");
        note.setContent("Test Note Content");
        note.setUser(owner);
        return note;
    }

    public static Note createNoteWithCategory(User owner, Category category) {
        Note note = createNote(owner);
        note.setCategory(category);
        return note;
    }

    public static Category createCategory() {
        Category category = new Category();
        category.setId(CATEGORY_ID);
        category.setTitle("Test Category");
        return category;
    }
}
