---
description: Schrijft complete unit tests voor een service klasse met 100% line coverage. Gebruik: /write-tests <ServiceImplClassName> (bijv. /write-tests NoteServiceImpl)
allowed-tools: Read, Write, Bash(./mvnw test:*), Glob, Grep
argument-hint: <ServiceImplClassName>
---

# Unit Tests Schrijven — $ARGUMENTS

Schrijf volledige unit tests voor `$ARGUMENTS` met **100% line coverage**.
Dit is een harde eis van de schoolopdracht (criterium 3.2).

## Stap 1 — Analyseer de service

Lees:
- `src/main/java/org/noteplus/noteplus/service/impl/$ARGUMENTS.java`
- De bijbehorende service interface
- De entities die de service gebruikt
- Bestaande exceptions in `src/main/java/org/noteplus/noteplus/exception/`

## Stap 2 — Bepaal welke scenarios getest moeten worden

Voor elke public methode in de service:
- **Happy path** — normaal geval, verwachte output
- **Niet gevonden** — `ResourceNotFoundException` als ID niet bestaat
- **Geen toegang** — `AccessDeniedException` als user niet eigenaar is
- **Validatie** — edge cases (null input, lege string)

Minimaal 5 test methodes per service klasse (school vereist 10 totaal over 2 klassen).

## Stap 3 — Schrijf de testklasse

Bestandslocatie: `src/test/java/org/noteplus/noteplus/service/$ARGUMENTS Test.java`

Volg dit patroon EXACT:

```java
@ExtendWith(MockitoExtension.class)
class $ARGUMENTSTest {

    // Mock alle dependencies
    @Mock private XxxRepository xxxRepository;
    @InjectMocks private $ARGUMENTS serviceUnderTest;

    // Test data constanten bovenaan
    private static final Long TEST_ID = 1L;
    private static final String TEST_USERNAME = "testuser@example.com";

    @Test
    @DisplayName("methodName - scenario - verwacht resultaat")
    void methodName_scenario_expectedResult() {
        // Arrange
        var entity = buildTestEntity();
        when(xxxRepository.findById(TEST_ID)).thenReturn(Optional.of(entity));

        // Act
        var result = serviceUnderTest.methodName(TEST_ID, TEST_USERNAME);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(TEST_ID);
        verify(xxxRepository).findById(TEST_ID);
    }

    @Test
    @DisplayName("methodName - entity not found - throws ResourceNotFoundException")
    void methodName_entityNotFound_throwsException() {
        // Arrange
        when(xxxRepository.findById(TEST_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> serviceUnderTest.methodName(TEST_ID, TEST_USERNAME))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("not found");
    }

    // Helper methode voor testdata — EIGEN data, nooit productiedata!
    private XxxEntity buildTestEntity() {
        var entity = new XxxEntity();
        entity.setId(TEST_ID);
        // ... zet alle velden
        return entity;
    }
}
```

## Stap 4 — Coverage check

Na het schrijven, run:
```bash
./mvnw test -Dtest=$ARGUMENTS Test
```

Als de test slaagt, controleer coverage:
```bash
./mvnw test jacoco:report
# Open: target/site/jacoco/index.html
```

Zorg dat de service klasse 100% line coverage heeft.
Als er lijnen zijn zonder coverage, schrijf extra tests voor die code paden.

## Stap 5 — Checklist

- [ ] Elke public methode heeft minimaal 1 happy path test
- [ ] Elke public methode heeft minimaal 1 exception test
- [ ] Alle tests gebruiken Arrange → Act → Assert
- [ ] Testdata is zelf gebouwd (geen productie-data)
- [ ] `@DisplayName` beschrijft het scenario duidelijk
- [ ] Geen `@SpringBootTest` in unit tests (dat is te zwaar, gebruik `@ExtendWith(MockitoExtension.class)`)
