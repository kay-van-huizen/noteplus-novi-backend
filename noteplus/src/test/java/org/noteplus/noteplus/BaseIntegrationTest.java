package org.noteplus.noteplus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared base for all integration tests.
 *
 * Spring Boot 4 note:
 *   @AutoConfigureMockMvc is provided by spring-boot-starter-webmvc-test.
 *   If IntelliJ cannot resolve it after a Maven reload, remove @AutoConfigureMockMvc
 *   from the class annotation and uncomment the MockMvcConfig inner class below.
 *
 * A single PostgreSQL Testcontainer is started once and reused across all subclasses
 * via the static @Container field + @ServiceConnection.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("noteplus_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // ── Fallback MockMvc wiring ────────────────────────────────────────────
    // If @AutoConfigureMockMvc cannot be resolved after a Maven reload:
    //   1. Remove @AutoConfigureMockMvc from the class annotation above
    //   2. Uncomment the @TestConfiguration block below
    //   3. Do Maven → Reload All Maven Projects in IntelliJ
    //
    // @TestConfiguration
    // static class MockMvcConfig {
    //     @Bean
    //     MockMvc mockMvc(WebApplicationContext context) {
    //         return MockMvcBuilders
    //                 .webAppContextSetup(context)
    //                 .apply(springSecurity())   // keeps the JWT filter active in tests
    //                 .build();
    //     }
    // }
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Registers a new user and returns their JWT token.
     *
     * RegisterRequest has fields: username, email, password (no 'name' field).
     * Uses HashMap instead of Map.of() for cleaner key-value construction.
     */
    protected String registerAndLogin(String username, String email, String password) throws Exception {
        Map<String, String> registerBody = new HashMap<>();
        registerBody.put("username", username);
        registerBody.put("email", email);
        registerBody.put("password", password);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated());

        Map<String, String> loginBody = new HashMap<>();
        loginBody.put("username", username);
        loginBody.put("password", password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }
}
