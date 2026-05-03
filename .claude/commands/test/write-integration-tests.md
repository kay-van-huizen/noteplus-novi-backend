---
description: Schrijft integratietests voor een controller via MockMvc en Testcontainers. Gebruik: /write-integration-tests <ControllerName> (bijv. /write-integration-tests NoteController)
allowed-tools: Read, Write, Glob, Grep
argument-hint: <ControllerName>
---

# Integratietests — $ARGUMENTS

Schrijf integratietests voor `$ARGUMENTS` via MockMvc.
De school vereist minimaal 2 geslaagde integratietests (criterium 3.2).

## Stap 1 — Lees de controller

Lees:
- `src/main/java/org/noteplus/noteplus/controller/$ARGUMENTS.java`
- De security config in `src/main/java/org/noteplus/noteplus/security/`
- `application.properties` voor de configuratie

## Stap 2 — Schrijf de integratietestklasse

Bestandslocatie: `src/test/java/org/noteplus/noteplus/controller/$ARGUMENTSIntegrationTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class $ARGUMENTSIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    // Helper: JWT token voor testgebruiker ophalen
    private String obtainToken(String username, String password) throws Exception {
        var loginRequest = Map.of("username", username, "password", password);
        var result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();
        var response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("token").asText();
    }

    @Test
    @DisplayName("GET endpoint - zonder token - 401 Unauthorized")
    void getEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/xxx"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET endpoint - met geldig token - 200 OK")
    void getEndpoint_withValidToken_returns200() throws Exception {
        String token = obtainToken("test@example.com", "password123");

        mockMvc.perform(get("/api/xxx")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST endpoint - met geldig token en valid body - 201 Created")
    void createEndpoint_validRequest_returns201() throws Exception {
        String token = obtainToken("test@example.com", "password123");
        var request = Map.of("title", "Test", "content", "Inhoud");

        mockMvc.perform(post("/api/xxx")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.title").value("Test"));
    }

    @Test
    @DisplayName("POST endpoint - zonder verplichte velden - 400 Bad Request")
    void createEndpoint_missingFields_returns400() throws Exception {
        String token = obtainToken("test@example.com", "password123");

        mockMvc.perform(post("/api/xxx")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}
```

## Stap 3 — Zorg dat testdata beschikbaar is

Controleer dat `src/main/resources/data.sql` een testgebruiker bevat met bekende credentials.
De integratietest heeft echte users nodig om tokens te genereren.

## Stap 4 — Run en valideer

```bash
./mvnw test -Dtest=$ARGUMENTSIntegrationTest
```

De test gebruikt Testcontainers om automatisch een PostgreSQL container te starten.
Zorg dat Docker actief is.
