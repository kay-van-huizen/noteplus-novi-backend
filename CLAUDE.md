# CLAUDE.md — NotePlus REST Web-API

This file is automatically loaded by Claude Code on every session.
It contains everything Claude needs to work effectively in this codebase.

---

## Quick Commands

```bash
./mvnw spring-boot:run          # Run application (port 8080)
./mvnw test                     # All tests
./mvnw test -Dtest=ClassName    # Single test class
./mvnw clean install            # Build
./mvnw package                  # Package JAR
```

Swagger UI: http://localhost:8080/swagger-ui/index.html

---

## Stack & Versions

| Tool | Version |
|------|---------|
| Java | 17 (LTS) |
| Spring Boot | 4.0.3 |
| Database | PostgreSQL 5432, database `Noteplus` |
| Auth | Spring Security + JWT (stateless) |
| Templates | JTE (hot-reload in dev) |
| API Docs | SpringDoc OpenAPI |
| Tests | JUnit 5 + Mockito + Testcontainers |

---

## Package Layout

```
org.noteplus.noteplus/
├── entity/         ← JPA domain model (all extend BaseEntity)
├── repository/     ← Spring Data JPA repositories
├── dto/            ← API request/response contracts (NEVER expose entities directly)
│   ├── request/    ← inkomende DTO's (bijv. CreateNoteRequest)
│   └── response/   ← uitgaande DTO's (bijv. NoteResponse)
├── service/        ← interfaces hier, implementaties in service/impl/
│   └── impl/
├── controller/     ← REST controllers
├── security/       ← JWT filter, service, config, UserDetailsService
├── config/         ← Spring config classes
└── exception/      ← custom exceptions + GlobalExceptionHandler
```

---

## Domain Model

```
User ──M:M── Role
User ──O:M── Note ──M:1── Category (self-referential, parent/child)
User ──O:M── LearningPath ──M:M── Note
Note ──M:M── Reference ──O:1── FileAttachment  ← one-to-one relatie (vereist!)
```

Alle entiteiten erven van `BaseEntity` (id UUID, createdAt, updatedAt).
`Note` heeft soft delete via `deletedAt` timestamp.
`FileAttachment` gebruikt orphan removal — verwijderd als Reference weg is.
Alle relaties zijn `LAZY` fetch.

---

## Architectuurregels (ALTIJD volgen)

### Service-laag patroon
```java
// Interface in service/
public interface NoteService {
    NoteResponse createNote(CreateNoteRequest request, String username);
    NoteResponse getNoteById(Long id, String username);
    List<NoteResponse> getAllNotesByUser(String username);
    NoteResponse updateNote(Long id, UpdateNoteRequest request, String username);
    void deleteNote(Long id, String username);
}

// Implementatie in service/impl/
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    // ...
}
```

### Controller-patroon
```java
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@Tag(name = "Notes", description = "Note management endpoints")
public class NoteController {

    private final NoteService noteService;

    @GetMapping
    @Operation(summary = "Get all notes for current user")
    @ApiResponse(responseCode = "200", description = "Notes retrieved")
    public ResponseEntity<List<NoteResponse>> getAllNotes(Authentication auth) {
        return ResponseEntity.ok(noteService.getAllNotesByUser(auth.getName()));
    }
}
```

### DTO-patroon
```java
// Request DTO — altijd valideren
public record CreateNoteRequest(
    @NotBlank @Size(max = 255) String title,
    @NotBlank String content,
    Long categoryId
) {}

// Response DTO — nooit de entity zelf returnen
public record NoteResponse(
    Long id,
    String title,
    String content,
    String categoryName,
    LocalDateTime createdAt
) {}
```

### Eigenaarcontrole — ALTIJD controleren
```java
// In service: controleer of de ingelogde user eigenaar is
private Note getNoteForUser(Long noteId, String username) {
    Note note = noteRepository.findById(noteId)
        .orElseThrow(() -> new ResourceNotFoundException("Note not found: " + noteId));
    if (!note.getUser().getUsername().equals(username)) {
        throw new AccessDeniedException("Not your note");
    }
    return note;
}
```

---

## Security

- Publieke endpoints: `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/error`
- Alle andere endpoints: `Authorization: Bearer <token>` verplicht
- Tokens verlopen na 24 uur (`app.jwt.expiration-ms=86400000`)
- Wachtwoorden: BCrypt
- Rollen: `ROLE_ADMIN`, `ROLE_STUDENT`, `ROLE_COACH`

Autorisatie in controller:
```java
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasAnyRole('STUDENT', 'COACH')")
@PreAuthorize("hasRole('ADMIN') or #username == authentication.name")
```

---

## Exception Handling

Gebruik deze custom exceptions (aanmaken in `exception/`):
- `ResourceNotFoundException extends RuntimeException` → 404
- `AccessDeniedException extends RuntimeException` → 403
- `DuplicateResourceException extends RuntimeException` → 409
- `ValidationException extends RuntimeException` → 400

`GlobalExceptionHandler` (`@ControllerAdvice`) handelt alles centraal af.
Nooit een try/catch in controller of service voor business logic.

---

## Testing — Schoolvereisten

De school eist:
- **2 service klassen** met **100% line coverage**
- **Minimaal 10 unit tests** totaal (JUnit 5 + Mockito)
- **Minimaal 2 integratietests** (MockMvc + Testcontainers)
- **Eigen testdata** in elke test, GEEN productie-data
- **Drie A's**: Arrange → Act → Assert structuur

```java
// Unit test patroon
@ExtendWith(MockitoExtension.class)
class NoteServiceImplTest {

    @Mock private NoteRepository noteRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private NoteServiceImpl noteService;

    @Test
    @DisplayName("createNote - happy path - returns NoteResponse")
    void createNote_validRequest_returnsResponse() {
        // Arrange
        var user = TestDataFactory.createUser();
        var request = new CreateNoteRequest("Titel", "Inhoud", null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // Act
        var result = noteService.createNote(request, "testuser");

        // Assert
        assertThat(result.title()).isEqualTo("Titel");
        verify(noteRepository).save(any(Note.class));
    }
}
```

```java
// Integratie test patroon
@SpringBootTest
@AutoConfigureMockMvc
class NoteControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void createNote_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"test\"}"))
            .andExpect(status().isUnauthorized());
    }
}
```

---

## Data Seeding

Gebruik `src/main/resources/data.sql` voor automatische testdata:
- Minimaal 1 gebruiker per rol (ADMIN, STUDENT, COACH)
- Test wachtwoord altijd BCrypt-gehashed
- Seed ook categorieën en een paar notes

---

## File Upload/Download (VERPLICHT per opdracht!)

Kernfunctionaliteit: bestanden koppelen aan een `Reference` entity.
- Upload: `POST /api/references/{id}/attachment` (MultipartFile)
- Download: `GET /api/references/{id}/attachment`
- Sla bestanden op in `/uploads/` directory of als byte[] in DB
- `FileAttachment` heeft al een one-to-one relatie met `Reference` (vereist!)

---

## Opdracht Quick-Checks

Voordat je commit, check:
- [ ] Nieuwe endpoint heeft Swagger `@Operation` + `@ApiResponse`
- [ ] Inkomende DTO heeft `@Valid` annotaties
- [ ] Service methode controleert eigenaarschap
- [ ] Geen entity direct als response teruggegeven
- [ ] Exception handling via `GlobalExceptionHandler`, niet try/catch in service

---

## Git Workflow (Schoolvereisten)

- Minimaal **20 kleine, beschrijvende commits**
- Minimaal **5 pull requests** (één per feature) → main branch
- Branch naamgeving: `feat/note-crud`, `feat/auth-endpoints`, `feat/file-upload`
- Commit naamgeving: `feat: add NoteController CRUD endpoints`

---

## Naamgevingsconventies

| Component | Naamgeving | Voorbeeld |
|-----------|-----------|-----------|
| Entity | PascalCase | `Note`, `LearningPath` |
| Repository | `EntityRepository` | `NoteRepository` |
| Service interface | `EntityService` | `NoteService` |
| Service impl | `EntityServiceImpl` | `NoteServiceImpl` |
| Controller | `EntityController` | `NoteController` |
| Request DTO | `CreateEntityRequest` / `UpdateEntityRequest` | `CreateNoteRequest` |
| Response DTO | `EntityResponse` | `NoteResponse` |
| Test class | `EntityServiceImplTest` | `NoteServiceImplTest` |

---

## Wat NOG NIET geïmplementeerd is

- [ ] Controllers (leeg)
- [ ] Services (leeg)
- [ ] GlobalExceptionHandler
- [ ] File upload/download endpoint
- [ ] data.sql testdata
- [ ] Unit tests
- [ ] Integratietests
