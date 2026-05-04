---
description: Scaffold een complete nieuwe feature voor NotePlus — maakt service interface, serviceImpl, controller, request/response DTOs en een unit test klasse aan. Gebruik: /new-feature <EntityName> (bijv. /new-feature LearningPath)
allowed-tools: Read, Write, Bash(git checkout:*), Bash(git branch:*), Glob, Grep
argument-hint: <EntityName>
---

# Nieuwe Feature Scaffold — $ARGUMENTS

Maak een volledige, consistente feature aan voor de entiteit `$ARGUMENTS` in het NotePlus project.

## Stap 1 — Lees het project

Lees eerst:
- `CLAUDE.md` voor architectuurregels
- `src/main/java/org/noteplus/noteplus/entity/$ARGUMENTS.java` (als die bestaat)
- `src/main/java/org/noteplus/noteplus/repository/$ARGUMENTS Repository.java` (als die bestaat)
- Kijk naar een bestaande service en controller als voorbeeld (bijv. als er één bestaat)

## Stap 2 — Git branch aanmaken

```bash
git checkout -b feat/$ARGUMENTS-crud
```

## Stap 3 — Maak de volgende bestanden aan

### 1. Request DTOs
`src/main/java/org/noteplus/noteplus/dto/request/Create$ARGUMENTSRequest.java`
`src/main/java/org/noteplus/noteplus/dto/request/Update$ARGUMENTSRequest.java`

Gebruik Java `record`, voeg `@NotBlank`, `@NotNull`, `@Size` toe waar logisch.

### 2. Response DTO
`src/main/java/org/noteplus/noteplus/dto/response/$ARGUMENTSResponse.java`

Java `record`, bevat `id`, `createdAt` en alle relevante velden. GEEN entity referenties.

### 3. Service interface
`src/main/java/org/noteplus/noteplus/service/$ARGUMENTSService.java`

Methods: `create`, `getById`, `getAllByUser`, `update`, `delete`.
Alle methodes die gebruikers-specifiek zijn, accepteren een `String username` parameter.

### 4. Service implementatie
`src/main/java/org/noteplus/noteplus/service/impl/$ARGUMENTSServiceImpl.java`

- `@Service @RequiredArgsConstructor`
- Injecteert repository via constructor
- Bevat eigenaarcontrole (gooit `ForbiddenException` als user niet eigenaar is)
- Converteert entity ↔ DTO (geen MapStruct, handmatig mappen)
- Soft delete als entiteit `deletedAt` heeft, anders hard delete

### 5. Controller
`src/main/java/org/noteplus/noteplus/controller/$ARGUMENTSController.java`

- `@RestController @RequestMapping("/api/$arguments") @RequiredArgsConstructor`
- `@Tag(name = "$ARGUMENTS", description = "...")`
- CRUD endpoints met correcte HTTP methodes en statuscodes
- Elke method: `Authentication auth` parameter voor ingelogde user
- Swagger: `@Operation(summary = "...")` + `@ApiResponse` op elke endpoint

### 6. Unit test klasse (opzet)
`src/test/java/org/noteplus/noteplus/service/$ARGUMENTSServiceImplTest.java`

- `@ExtendWith(MockitoExtension.class)`
- Mocks voor alle repository dependencies
- Minimaal 5 test methodes als opzet (happy path + unhappy paths)
- Gebruik `@DisplayName` voor leesbaarheid
- Volg Arrange → Act → Assert structuur

## Stap 4 — Controleer

Na aanmaken:
1. Controleer dat alle imports correct zijn
2. Controleer dat de controller de service interface gebruikt (niet de impl)
3. Controleer dat `GlobalExceptionHandler` de custom exceptions afhandelt
4. Run `./mvnw compile` om te zien of alles compileert

## Naamgeving regels

- Entity: `$ARGUMENTS` (PascalCase, enkelvoud)
- URL pad: lowercase, meervoud (bijv. `LearningPath` → `/api/learning-paths`)
- Package: `org.noteplus.noteplus`
