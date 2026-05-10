---
description: Writes complete unit tests for NoteServiceImpl targeting 100% line coverage. Uses JUnit 5 + Mockito. Run after NoteServiceImpl is fully implemented. School requirement: 2 service classes at 100% coverage, minimum 10 unit tests total using Arrange-Act-Assert.
allowed-tools: Read, Write, Bash(./mvnw test:*), Bash(./mvnw verify:*), Glob, Grep
---

# Unit Tests — NoteServiceImpl

## School requirements covered by this file

- ✅ 1 of 2 required service classes at 100% line coverage
- ✅ Minimum 7 unit tests (combined with auth-service tests = 14+ total)
- ✅ Arrange → Act → Assert structure on every test
- ✅ Own test data — no production data used

## Step 0 — Read context first (MANDATORY)

Before writing any test code, read:

```
src/main/java/org/noteplus/noteplus/service/impl/NoteServiceImpl.java   ← every method + every branch
src/main/java/org/noteplus/noteplus/service/NoteService.java
src/main/java/org/noteplus/noteplus/entity/Note.java                    ← fields, deletedAt type
src/main/java/org/noteplus/noteplus/entity/User.java
src/main/java/org/noteplus/noteplus/entity/Category.java
src/main/java/org/noteplus/noteplus/repository/NoteRepository.java      ← custom query method names
src/main/java/org/noteplus/noteplus/repository/UserRepository.java
src/main/java/org/noteplus/noteplus/repository/CategoryRepository.java
src/main/java/org/noteplus/noteplus/dto/request/CreateNoteRequest.java
src/main/java/org/noteplus/noteplus/dto/request/UpdateNoteRequest.java
src/main/java/org/noteplus/noteplus/dto/response/NoteResponse.java
src/main/java/org/noteplus/noteplus/exception/ForbiddenException.java
src/main/java/org/noteplus/noteplus/exception/ResourceNotFoundException.java
```

Map every branch in NoteServiceImpl before writing a single test.
Every `if` statement = at least two tests (true branch + false branch).
Every `throw` statement = at least one test that triggers it.

## Step 2 — Create TestDataFactory

Create `src/test/java/org/noteplus/noteplus/util/TestDataFactory.java`:

```java
package org.noteplus.noteplus.util;

import org.noteplus.noteplus.entity.Category;
import org.noteplus.noteplus.entity.Note;
import org.noteplus.noteplus.entity.Role;
import org.noteplus.noteplus.entity.User;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Central factory for test data.
 * All test data is defined here — never use production data in tests.
 */
public class TestDataFactory {

    // ── Users ────────────────────────────────────────────────────────────

    public static User createStudent() {
        User user = new User();
        user.setId(1L);
        user.setUsername("student1");
        user.setEmail("student1@test.nl");
        user.setPassword("$2a$10$hashedpassword");
        user.setName("Test Student");
        return user;
    }

    public static User createCoach() {
        User user = new User();
        user.setId(2L);
        user.setUsername("coach1");
        user.setEmail("coach1@test.nl");
        user.setPassword("$2a$10$hashedpassword");
        user.setName("Test Coach");
        return user;
    }

    public static User createAdmin() {
        User user = new User();
        user.setId(3L);
        user.setUsername("admin");
        user.setEmail("admin@test.nl");
        user.setPassword("$2a$10$hashedpassword");
        user.setName("Test Admin");
        return user;
    }

    // ── Roles ─────────────────────────────────────────────────────────────

    public static Role createStudentRole() {
        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_STUDENT");
        return role;
    }

    public static Role createCoachRole() {
        Role role = new Role();
        role.setId(2L);
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
}
```

## Step 3 — Write NoteServiceImplTest

Create `src/test/java/org/noteplus/noteplus/service/NoteServiceImplTest.java`:

```java
package org.noteplus.noteplus.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.noteplus.noteplus.dto.request.CreateNoteRequest;
import org.noteplus.noteplus.dto.request.UpdateNoteRequest;
import org.noteplus.noteplus.dto.response.NoteResponse;
import org.noteplus.noteplus.entity.Category;
import org.noteplus.noteplus.entity.Note;
import org.noteplus.noteplus.entity.User;
import org.noteplus.noteplus.exception.ForbiddenException;
import org.noteplus.noteplus.exception.ResourceNotFoundException;
import org.noteplus.noteplus.repository.CategoryRepository;
import org.noteplus.noteplus.repository.NoteRepository;
import org.noteplus.noteplus.repository.UserRepository;
import org.noteplus.noteplus.service.impl.NoteServiceImpl;
import org.noteplus.noteplus.util.TestDataFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NoteServiceImpl.
 * Target: 100% line coverage.
 * Pattern: Arrange → Act → Assert on every test.
 * Test data: TestDataFactory only — no production data.
 */
@ExtendWith(MockitoExtension.class)
class NoteServiceImplTest {

    @Mock private NoteRepository noteRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @InjectMocks private NoteServiceImpl noteService;

    // ── create() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("create - valid request without category - returns NoteResponse")
    void create_validRequestNoCategoryId_returnsNoteResponse() {
        // Arrange
        User student = TestDataFactory.createStudent();
        CreateNoteRequest request = new CreateNoteRequest("My Title", "My Content", null);
        Note savedNote = TestDataFactory.createNote(student);

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(noteRepository.save(any(Note.class))).thenReturn(savedNote);

        // Act
        NoteResponse result = noteService.create(request, "student1");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Test Note Title");
        assertThat(result.ownerUsername()).isEqualTo("student1");
        assertThat(result.categoryId()).isNull();
        verify(noteRepository).save(any(Note.class));
    }

    @Test
    @DisplayName("create - valid request with existing category - links category to note")
    void create_validRequestWithCategoryId_linksCategoryAndReturnsResponse() {
        // Arrange
        User student = TestDataFactory.createStudent();
        Category category = TestDataFactory.createCategory();
        CreateNoteRequest request = new CreateNoteRequest("My Title", "My Content", 10L);
        Note savedNote = TestDataFactory.createNoteWithCategory(student, category);

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(noteRepository.save(any(Note.class))).thenReturn(savedNote);

        // Act
        NoteResponse result = noteService.create(request, "student1");

        // Assert
        assertThat(result.categoryId()).isEqualTo(10L);
        assertThat(result.categoryTitle()).isEqualTo("Test Category");
        verify(categoryRepository).findById(10L);
    }

    @Test
    @DisplayName("create - user not found - throws ResourceNotFoundException")
    void create_userNotFound_throwsResourceNotFoundException() {
        // Arrange
        CreateNoteRequest request = new CreateNoteRequest("Title", "Content", null);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> noteService.create(request, "ghost"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(noteRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - category not found - throws ResourceNotFoundException")
    void create_categoryNotFound_throwsResourceNotFoundException() {
        // Arrange
        User student = TestDataFactory.createStudent();
        CreateNoteRequest request = new CreateNoteRequest("Title", "Content", 999L);

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> noteService.create(request, "student1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");

        verify(noteRepository, never()).save(any());
    }

    // ── getById() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById - owner requests own note - returns NoteResponse")
    void getById_ownerRequestsOwnNote_returnsResponse() {
        // Arrange
        User student = TestDataFactory.createStudent();
        Note note = TestDataFactory.createNote(student);

        when(noteRepository.findByIdNotDeleted(100L)).thenReturn(Optional.of(note));

        // Act
        NoteResponse result = noteService.getById(100L, "student1");

        // Assert
        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.title()).isEqualTo("Test Note Title");
    }

    @Test
    @DisplayName("getById - non-owner requests note - throws ForbiddenException")
    void getById_nonOwnerRequestsNote_throwsForbiddenException() {
        // Arrange
        User student = TestDataFactory.createStudent();
        Note note = TestDataFactory.createNote(student);

        when(noteRepository.findByIdNotDeleted(100L)).thenReturn(Optional.of(note));

        // Act & Assert
        assertThatThrownBy(() -> noteService.getById(100L, "coach1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("access");
    }

    @Test
    @DisplayName("getById - note does not exist - throws ResourceNotFoundException")
    void getById_noteNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(noteRepository.findByIdNotDeleted(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> noteService.getById(999L, "student1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Note not found");
    }

    // ── getAllForUser() ────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllForUser - user with notes - returns only own non-deleted notes")
    void getAllForUser_userHasNotes_returnsOwnNotes() {
        // Arrange
        User student = TestDataFactory.createStudent();
        List<Note> notes = List.of(
                TestDataFactory.createNote(student),
                TestDataFactory.createNote(student)
        );

        when(noteRepository.findAllByUsernameNotDeleted("student1")).thenReturn(notes);

        // Act
        List<NoteResponse> result = noteService.getAllForUser("student1");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(n -> n.ownerUsername().equals("student1"));
    }

    @Test
    @DisplayName("getAllForUser - user with no notes - returns empty list")
    void getAllForUser_userHasNoNotes_returnsEmptyList() {
        // Arrange
        when(noteRepository.findAllByUsernameNotDeleted("student1")).thenReturn(List.of());

        // Act
        List<NoteResponse> result = noteService.getAllForUser("student1");

        // Assert
        assertThat(result).isEmpty();
    }

    // ── update() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("update - owner updates own note - saves and returns updated response")
    void update_ownerUpdatesNote_returnsupdatedResponse() {
        // Arrange
        User student = TestDataFactory.createStudent();
        Note note = TestDataFactory.createNote(student);
        UpdateNoteRequest request = new UpdateNoteRequest("Updated Title", "Updated Content", null);

        when(noteRepository.findByIdNotDeleted(100L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        NoteResponse result = noteService.update(100L, request, "student1");

        // Assert
        assertThat(result.title()).isEqualTo("Updated Title");
        assertThat(result.content()).isEqualTo("Updated Content");
        verify(noteRepository).save(note);
    }

    @Test
    @DisplayName("update - non-owner tries to update - throws ForbiddenException")
    void update_nonOwnerAttemptsUpdate_throwsForbiddenException() {
        // Arrange
        User student = TestDataFactory.createStudent();
        Note note = TestDataFactory.createNote(student);
        UpdateNoteRequest request = new UpdateNoteRequest("Hacked Title", "Hacked Content", null);

        when(noteRepository.findByIdNotDeleted(100L)).thenReturn(Optional.of(note));

        // Act & Assert
        assertThatThrownBy(() -> noteService.update(100L, request, "coach1"))
                .isInstanceOf(ForbiddenException.class);

        verify(noteRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - note not found - throws ResourceNotFoundException")
    void update_noteNotFound_throwsResourceNotFoundException() {
        // Arrange
        UpdateNoteRequest request = new UpdateNoteRequest("Title", "Content", null);
        when(noteRepository.findByIdNotDeleted(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> noteService.update(999L, request, "student1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete() — soft delete ────────────────────────────────────────────

    @Test
    @DisplayName("delete - owner deletes own note - sets deletedAt and saves")
    void delete_ownerDeletesNote_setsDeletedAtAndSaves() {
        // Arrange
        User student = TestDataFactory.createStudent();
        Note note = TestDataFactory.createNote(student);

        when(noteRepository.findByIdNotDeleted(100L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        // Act
        noteService.delete(100L, "student1");

        // Assert — capture the saved note and verify deletedAt was set
        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
        assertThat(captor.getValue().getDeletedAt()).isBeforeOrEqualTo(LocalDateTime.now());

        // Verify hard delete was NOT called
        verify(noteRepository, never()).delete(any(Note.class));
        verify(noteRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete - non-owner tries to delete - throws ForbiddenException")
    void delete_nonOwnerAttemptsDelete_throwsForbiddenException() {
        // Arrange
        User student = TestDataFactory.createStudent();
        Note note = TestDataFactory.createNote(student);

        when(noteRepository.findByIdNotDeleted(100L)).thenReturn(Optional.of(note));

        // Act & Assert
        assertThatThrownBy(() -> noteService.delete(100L, "coach1"))
                .isInstanceOf(ForbiddenException.class);

        verify(noteRepository, never()).save(any());
        verify(noteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete - note not found - throws ResourceNotFoundException")
    void delete_noteNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(noteRepository.findByIdNotDeleted(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> noteService.delete(999L, "student1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

## Step 4 — Run tests and check coverage

[//]: # (```bash)
[//]: # (./mvnw test -Dtest=NoteServiceImplTest)
[//]: # (```)

[//]: # ()
[//]: # (All tests must be GREEN before checking coverage.)

[//]: # ()
[//]: # (Then run with JaCoCo:)

[//]: # (```bash)
[//]: # (./mvnw verify)
[//]: # (```)

Open `target/site/jacoco/index.html` in your browser.
Navigate to `NoteServiceImpl` and verify:
- Line coverage: 100%
- Branch coverage: as high as possible (aim for 80%+)

If any line is RED (not covered):
- Read what that line does
- Write an additional test that exercises that specific path
- Re-run until 100% line coverage is achieved

## Step 5 — Checklist before committing

- [ ] All tests use TestDataFactory — no hardcoded magic strings outside the factory
- [ ] Every test has exactly three sections: Arrange, Act, Assert (with comments)
- [ ] Every `throw` in NoteServiceImpl has a corresponding test
- [ ] `verify(noteRepository, never()).delete(any())` is present in the soft delete test
- [ ] No `@SpringBootTest` annotation — these are pure unit tests
- [ ] `./mvnw test -Dtest=NoteServiceImplTest` runs green without starting the full app
