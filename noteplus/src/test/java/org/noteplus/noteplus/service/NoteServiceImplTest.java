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
import org.noteplus.noteplus.entity.Note;
import org.noteplus.noteplus.exception.ForbiddenException;
import org.noteplus.noteplus.exception.ResourceNotFoundException;
import org.noteplus.noteplus.repository.CategoryRepository;
import org.noteplus.noteplus.repository.NoteRepository;
import org.noteplus.noteplus.repository.UserRepository;
import org.noteplus.noteplus.service.impl.NoteServiceImpl;
import org.noteplus.noteplus.util.TestDataFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceImplTest {

    @Mock NoteRepository noteRepository;
    @Mock UserRepository userRepository;
    @Mock CategoryRepository categoryRepository;
    @InjectMocks NoteServiceImpl noteService;

    // ── create() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create - valid request without categoryId - returns NoteResponse, categoryRepository never called")
    void create_validRequestNoCategoryId_returnsNoteResponseWithoutCategory() {
        // Arrange
        var student = TestDataFactory.createStudent();
        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> {
            Note n = inv.getArgument(0);
            n.setId(TestDataFactory.NOTE_ID);
            return n;
        });

        // Act
        NoteResponse result = noteService.create(new CreateNoteRequest("My Title", "My Content", null), "student1");

        // Assert
        assertThat(result.title()).isEqualTo("My Title");
        assertThat(result.ownerUsername()).isEqualTo("student1");
        assertThat(result.categoryId()).isNull();
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("create - valid request with categoryId - links category, response has categoryId and categoryTitle")
    void create_validRequestWithCategoryId_linksCategoryInResponse() {
        // Arrange
        var student = TestDataFactory.createStudent();
        var category = TestDataFactory.createCategory();
        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(categoryRepository.findById(TestDataFactory.CATEGORY_ID)).thenReturn(Optional.of(category));
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> {
            Note n = inv.getArgument(0);
            n.setId(TestDataFactory.NOTE_ID);
            return n;
        });

        // Act
        NoteResponse result = noteService.create(
                new CreateNoteRequest("My Title", "My Content", TestDataFactory.CATEGORY_ID), "student1");

        // Assert
        assertThat(result.categoryId()).isEqualTo(TestDataFactory.CATEGORY_ID);
        assertThat(result.categoryTitle()).isEqualTo("Test Category");
        verify(categoryRepository).findById(TestDataFactory.CATEGORY_ID);
    }

    @Test
    @DisplayName("create - user not found - throws ResourceNotFoundException with 'User not found', save never called")
    void create_userNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> noteService.create(new CreateNoteRequest("Title", "Content", null), "ghost"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
        verify(noteRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - category not found - throws ResourceNotFoundException with 'Category not found', save never called")
    void create_categoryNotFound_throwsResourceNotFoundException() {
        // Arrange
        var student = TestDataFactory.createStudent();
        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(categoryRepository.findById(TestDataFactory.NOT_FOUND_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> noteService.create(
                new CreateNoteRequest("Title", "Content", TestDataFactory.NOT_FOUND_ID), "student1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
        verify(noteRepository, never()).save(any());
    }

    // ── getById() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById - owner requests own note - returns NoteResponse with correct id, title, ownerUsername")
    void getById_ownerRequestsOwnNote_returnsNoteResponse() {
        // Arrange
        var student = TestDataFactory.createStudent();
        var note = TestDataFactory.createNote(student);
        when(noteRepository.findByIdNotDeleted(TestDataFactory.NOTE_ID)).thenReturn(Optional.of(note));

        // Act
        NoteResponse result = noteService.getById(TestDataFactory.NOTE_ID, "student1");

        // Assert
        assertThat(result.id()).isEqualTo(TestDataFactory.NOTE_ID);
        assertThat(result.title()).isEqualTo("Test Note Title");
        assertThat(result.ownerUsername()).isEqualTo("student1");
    }

    @Test
    @DisplayName("getById - non-owner requests note - throws ForbiddenException with 'access' in message")
    void getById_nonOwnerRequestsNote_throwsForbiddenException() {
        // Arrange
        var student = TestDataFactory.createStudent();
        var note = TestDataFactory.createNote(student);
        when(noteRepository.findByIdNotDeleted(TestDataFactory.NOTE_ID)).thenReturn(Optional.of(note));
        // userRepository returns empty Optional by default → isAdmin() returns false

        // Act & Assert
        assertThatThrownBy(() -> noteService.getById(TestDataFactory.NOTE_ID, "student2"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("access");
    }

    @Test
    @DisplayName("getById - note does not exist - throws ResourceNotFoundException with 'Note not found'")
    void getById_noteNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(noteRepository.findByIdNotDeleted(TestDataFactory.NOT_FOUND_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> noteService.getById(TestDataFactory.NOT_FOUND_ID, "student1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Note not found");
    }

    // ── getAllForUser() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllForUser - user has 2 notes - returns list of size 2, all with ownerUsername 'student1'")
    void getAllForUser_twoNotes_returnsListOfTwoWithCorrectOwner() {
        // Arrange
        var student = TestDataFactory.createStudent();
        var note1 = TestDataFactory.createNote(student);
        var note2 = new Note();
        note2.setId(UUID.randomUUID());
        note2.setTitle("Second Note");
        note2.setContent("Content 2");
        note2.setUser(student);
        when(noteRepository.findAllByUsernameNotDeleted("student1")).thenReturn(List.of(note1, note2));

        // Act
        List<NoteResponse> result = noteService.getAllForUser("student1");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.ownerUsername().equals("student1"));
    }

    @Test
    @DisplayName("getAllForUser - user has no notes - returns empty list")
    void getAllForUser_noNotes_returnsEmptyList() {
        // Arrange
        when(noteRepository.findAllByUsernameNotDeleted("student1")).thenReturn(List.of());

        // Act
        List<NoteResponse> result = noteService.getAllForUser("student1");

        // Assert
        assertThat(result).isEmpty();
    }

    // ── getAll() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll - returns all non-deleted notes across all users (student + coach = size 2)")
    void getAll_returnsAllNonDeletedNotes() {
        // Arrange
        var student = TestDataFactory.createStudent();
        var coach = TestDataFactory.createCoach();
        var studentNote = TestDataFactory.createNote(student);
        var coachNote = new Note();
        coachNote.setId(UUID.randomUUID());
        coachNote.setTitle("Coach Note");
        coachNote.setContent("Content");
        coachNote.setUser(coach);
        when(noteRepository.findAllNotDeleted()).thenReturn(List.of(studentNote, coachNote));

        // Act
        List<NoteResponse> result = noteService.getAll();

        // Assert
        assertThat(result).hasSize(2);
    }

    // ── getByCategoryId() ───────────────────────────────────────────────────

    @Test
    @DisplayName("getByCategoryId - returns notes matching the category with correct categoryId in response")
    void getByCategoryId_returnsNotesMatchingCategory() {
        // Arrange
        var student = TestDataFactory.createStudent();
        var category = TestDataFactory.createCategory();
        var note = TestDataFactory.createNoteWithCategory(student, category);
        when(noteRepository.findByCategoryIdNotDeleted(TestDataFactory.CATEGORY_ID)).thenReturn(List.of(note));

        // Act
        List<NoteResponse> result = noteService.getByCategoryId(TestDataFactory.CATEGORY_ID);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).categoryId()).isEqualTo(TestDataFactory.CATEGORY_ID);
    }

    // ── update() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update - owner updates with null categoryId - category cleared, title/content updated, categoryRepository never called")
    void update_ownerWithNullCategoryId_clearsCategoryAndUpdatesFields() {
        // Arrange
        var student = TestDataFactory.createStudent();
        var note = TestDataFactory.createNote(student);
        when(noteRepository.findByIdNotDeleted(TestDataFactory.NOTE_ID)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        NoteResponse result = noteService.update(
                TestDataFactory.NOTE_ID, new UpdateNoteRequest("Updated Title", "Updated Content", null), "student1");

        // Assert
        assertThat(result.title()).isEqualTo("Updated Title");
        assertThat(result.content()).isEqualTo("Updated Content");
        assertThat(result.categoryId()).isNull();
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("update - owner updates with valid categoryId - category linked, response has categoryId and categoryTitle")
    void update_ownerWithValidCategoryId_linksCategoryInResponse() {
        // Arrange
        var student = TestDataFactory.createStudent();
        var category = TestDataFactory.createCategory();
        var note = TestDataFactory.createNote(student);
        when(noteRepository.findByIdNotDeleted(TestDataFactory.NOTE_ID)).thenReturn(Optional.of(note));
        when(categoryRepository.findById(TestDataFactory.CATEGORY_ID)).thenReturn(Optional.of(category));
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        NoteResponse result = noteService.update(
                TestDataFactory.NOTE_ID,
                new UpdateNoteRequest("Updated Title", "Updated Content", TestDataFactory.CATEGORY_ID),
                "student1");

        // Assert
        assertThat(result.categoryId()).isEqualTo(TestDataFactory.CATEGORY_ID);
        assertThat(result.categoryTitle()).isEqualTo("Test Category");
    }

    @Test
    @DisplayName("update - category not found during update - throws ResourceNotFoundException, save never called")
    void update_categoryNotFound_throwsResourceNotFoundException() {
        // Arrange
        var student = TestDataFactory.createStudent();
        var note = TestDataFactory.createNote(student);
        when(noteRepository.findByIdNotDeleted(TestDataFactory.NOTE_ID)).thenReturn(Optional.of(note));
        when(categoryRepository.findById(TestDataFactory.NOT_FOUND_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> noteService.update(
                TestDataFactory.NOTE_ID,
                new UpdateNoteRequest("Title", "Content", TestDataFactory.NOT_FOUND_ID),
                "student1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
        verify(noteRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - non-owner tries to update - throws ForbiddenException with 'own' in message, save never called")
    void update_nonOwner_throwsForbiddenException() {
        // Arrange
        var student = TestDataFactory.createStudent();
        var note = TestDataFactory.createNote(student);
        when(noteRepository.findByIdNotDeleted(TestDataFactory.NOTE_ID)).thenReturn(Optional.of(note));
        // userRepository returns empty Optional by default → isAdmin() returns false

        // Act & Assert
        assertThatThrownBy(() -> noteService.update(
                TestDataFactory.NOTE_ID, new UpdateNoteRequest("Title", "Content", null), "student2"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own");
        verify(noteRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - note not found - throws ResourceNotFoundException with 'Note not found'")
    void update_noteNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(noteRepository.findByIdNotDeleted(TestDataFactory.NOT_FOUND_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> noteService.update(
                TestDataFactory.NOT_FOUND_ID, new UpdateNoteRequest("Title", "Content", null), "student1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Note not found");
    }

    // ── delete() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete - owner soft-deletes own note - deletedAt set and not null, delete/deleteById never called")
    void delete_ownerSoftDeletesNote_deletedAtSetNeverHardDeleted() {
        // Arrange
        var student = TestDataFactory.createStudent();
        var note = TestDataFactory.createNote(student);
        var captor = ArgumentCaptor.forClass(Note.class);
        when(noteRepository.findByIdNotDeleted(TestDataFactory.NOTE_ID)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        noteService.delete(TestDataFactory.NOTE_ID, "student1");

        // Assert
        verify(noteRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
        verify(noteRepository, never()).delete(any());
        verify(noteRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete - non-owner tries to delete - throws ForbiddenException with 'own' in message, save never called")
    void delete_nonOwner_throwsForbiddenException() {
        // Arrange
        var student = TestDataFactory.createStudent();
        var note = TestDataFactory.createNote(student);
        when(noteRepository.findByIdNotDeleted(TestDataFactory.NOTE_ID)).thenReturn(Optional.of(note));
        // userRepository returns empty Optional by default → isAdmin() returns false

        // Act & Assert
        assertThatThrownBy(() -> noteService.delete(TestDataFactory.NOTE_ID, "student2"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own");
        verify(noteRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete - note not found - throws ResourceNotFoundException with 'Note not found'")
    void delete_noteNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(noteRepository.findByIdNotDeleted(TestDataFactory.NOT_FOUND_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> noteService.delete(TestDataFactory.NOT_FOUND_ID, "student1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Note not found");
    }
}