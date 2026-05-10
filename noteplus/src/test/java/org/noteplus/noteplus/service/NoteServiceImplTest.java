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
        verify(categoryRepository, never()).findById(any());
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
        assertThat(result.ownerUsername()).isEqualTo("student1");
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

    // ── getAll() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll - returns all non-deleted notes across all users")
    void getAll_returnsAllNonDeletedNotes() {
        // Arrange
        User student = TestDataFactory.createStudent();
        User coach = TestDataFactory.createCoach();
        List<Note> notes = List.of(
                TestDataFactory.createNote(student),
                TestDataFactory.createNote(coach)
        );

        when(noteRepository.findAllNotDeleted()).thenReturn(notes);

        // Act
        List<NoteResponse> result = noteService.getAll();

        // Assert
        assertThat(result).hasSize(2);
    }

    // ── getByCategoryId() ─────────────────────────────────────────────────

    @Test
    @DisplayName("getByCategoryId - returns all notes in the given category")
    void getByCategoryId_returnsNotesForCategory() {
        // Arrange
        User student = TestDataFactory.createStudent();
        Category category = TestDataFactory.createCategory();
        Note note = TestDataFactory.createNoteWithCategory(student, category);

        when(noteRepository.findByCategoryIdNotDeleted(10L)).thenReturn(List.of(note));

        // Act
        List<NoteResponse> result = noteService.getByCategoryId(10L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).categoryId()).isEqualTo(10L);
    }

    // ── update() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("update - owner updates note with null categoryId - clears category and saves")
    void update_ownerUpdatesNoteWithNullCategory_clearsCategoryAndReturnsResponse() {
        // Arrange
        User student = TestDataFactory.createStudent();
        Note note = TestDataFactory.createNote(student);
        UpdateNoteRequest request = new UpdateNoteRequest("Updated Title", "Updated Content", null);

        when(noteRepository.findByIdNotDeleted(100L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        NoteResponse result = noteService.update(100L, request, "student1");

        // Assert — title and content updated, category cleared
        assertThat(result.title()).isEqualTo("Updated Title");
        assertThat(result.content()).isEqualTo("Updated Content");
        assertThat(result.categoryId()).isNull();
        verify(noteRepository).save(note);
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("update - owner updates note with valid categoryId - links new category")
    void update_ownerUpdatesNoteWithCategoryId_linksCategoryAndReturnsResponse() {
        // Arrange
        User student = TestDataFactory.createStudent();
        Category category = TestDataFactory.createCategory();
        Note note = TestDataFactory.createNote(student);
        UpdateNoteRequest request = new UpdateNoteRequest("Updated Title", "Updated Content", 10L);

        when(noteRepository.findByIdNotDeleted(100L)).thenReturn(Optional.of(note));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(noteRepository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        NoteResponse result = noteService.update(100L, request, "student1");

        // Assert
        assertThat(result.categoryId()).isEqualTo(10L);
        assertThat(result.categoryTitle()).isEqualTo("Test Category");
        verify(categoryRepository).findById(10L);
    }

    @Test
    @DisplayName("update - category not found during update - throws ResourceNotFoundException")
    void update_categoryNotFound_throwsResourceNotFoundException() {
        // Arrange
        User student = TestDataFactory.createStudent();
        Note note = TestDataFactory.createNote(student);
        UpdateNoteRequest request = new UpdateNoteRequest("Title", "Content", 999L);

        when(noteRepository.findByIdNotDeleted(100L)).thenReturn(Optional.of(note));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> noteService.update(100L, request, "student1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");

        verify(noteRepository, never()).save(any());
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
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own");

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
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Note not found");
    }

    // ── delete() — soft delete ────────────────────────────────────────────

    @Test
    @DisplayName("delete - owner deletes own note - sets deletedAt timestamp and saves")
    void delete_ownerDeletesNote_setsDeletedAtAndSaves() {
        // Arrange
        User student = TestDataFactory.createStudent();
        Note note = TestDataFactory.createNote(student);

        when(noteRepository.findByIdNotDeleted(100L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        // Act
        noteService.delete(100L, "student1");

        // Assert — soft delete: deletedAt must be set to a recent timestamp
        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
        assertThat(captor.getValue().getDeletedAt()).isBeforeOrEqualTo(LocalDateTime.now());

        // Hard delete must NEVER be called
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
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own");

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
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Note not found");
    }
}
