---
description: Configures SpringDoc OpenAPI with JWT Bearer authentication scheme, global security requirement, and API metadata. Enables the Authorize button in Swagger UI so all secured endpoints can be tested without Postman.
allowed-tools: Read, Write, Bash(./mvnw compile:*), Glob, Grep
---

# OpenAPI + Swagger JWT Config — NotePlus

## Why this matters

Every controller already has `@SecurityRequirement(name = "bearerAuth")`.
Without this config class, that annotation resolves to nothing — Swagger has no
security scheme named "bearerAuth" to reference. The Authorize button will not appear.

This config also defines global API metadata (title, version, description) that appears
at the top of the Swagger UI page and in the exported OpenAPI JSON spec.

## Security considerations before writing any code

**🔴 Do NOT expose stack traces in Swagger.**
Verify that `server.error.include-stacktrace` is set to `never` in application.properties.
Stack traces in API error responses leak internal class names and package structure.

**🔴 Do NOT include sensitive endpoints in the Swagger spec in production.**
For this school project, Swagger is enabled in all environments.
In a real production setup you would disable it outside dev/test.
Add a comment in the config acknowledging this as a known limitation.

**🟡 The security scheme name must match exactly.**
Every `@SecurityRequirement(name = "bearerAuth")` in every controller depends on
the scheme name being exactly `"bearerAuth"`. If it differs, the padlock icons
in Swagger will appear but authorization will silently not apply.
After building, verify by reading all controllers and confirming the name matches.

**🟡 Swagger UI itself must remain a public endpoint.**
`/swagger-ui/**` and `/v3/api-docs/**` must stay in SecurityConfig's permitAll() list.
If they are accidentally removed, Swagger becomes inaccessible.
Verify SecurityConfig after building — do not modify it as part of this task.

## Step 0 — Read context first

Before writing any code, read:

```
src/main/java/org/noteplus/noteplus/security/SecurityConfig.java
  → confirm /swagger-ui/** and /v3/api-docs/** are in permitAll()
  → confirm @EnableMethodSecurity is present

src/main/java/org/noteplus/noteplus/controller/AuthController.java
src/main/java/org/noteplus/noteplus/controller/NoteController.java
src/main/java/org/noteplus/noteplus/controller/CategoryController.java
  → confirm @SecurityRequirement(name = "bearerAuth") is present on secured controllers
  → confirm @Tag annotations are present (these populate the Swagger grouping)

src/main/resources/application.properties
  → check for existing springdoc.* properties
  → check server.error.include-stacktrace setting

pom.xml
  → confirm springdoc-openapi-starter-webmvc-ui is present with version 3.x
```

## Step 2 — Fix application.properties

Add or verify these properties:

```properties
# Swagger UI settings
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
springdoc.swagger-ui.try-it-out-enabled=true
springdoc.swagger-ui.filter=true

# Never expose stack traces in API error responses
server.error.include-stacktrace=never
server.error.include-message=always
server.error.include-binding-errors=always
```

`springdoc.swagger-ui.try-it-out-enabled=true` enables the "Try it out" button
per endpoint by default — without this, testers must click it manually every time.

`springdoc.swagger-ui.filter=true` adds a search bar to Swagger UI,
useful once you have 20+ endpoints.

## Step 3 — Create OpenApiConfig

`src/main/java/org/noteplus/noteplus/config/OpenApiConfig.java`:

```java
package org.noteplus.noteplus.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI notePlusOpenAPI() {
        // Security scheme — defines what "bearerAuth" means
        // This is what @SecurityRequirement(name = "bearerAuth") refers to
        SecurityScheme bearerScheme = new SecurityScheme()
                .name(BEARER_AUTH)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste your JWT token here. Obtain it from POST /api/auth/login.");

        // Global security requirement — applies bearerAuth to all endpoints by default
        // Individual public endpoints override this with @SecurityRequirement(name = "")
        SecurityRequirement globalSecurity = new SecurityRequirement()
                .addList(BEARER_AUTH);

        return new OpenAPI()
                .info(buildApiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server")
                ))
                .addSecurityItem(globalSecurity)
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, bearerScheme));
    }

    private Info buildApiInfo() {
        return new Info()
                .title("NotePlus API")
                .version("1.0.0")
                .description("""
                        NotePlus REST API — a note management platform for students and coaches.

                        ## Authentication
                        1. Register via `POST /api/auth/register`
                        2. Login via `POST /api/auth/login` — copy the returned token
                        3. Click the **Authorize** button (🔒) at the top of this page
                        4. Paste the token (without the word "Bearer") and click Authorize
                        5. All secured endpoints will now include your token automatically

                        ## Roles
                        - **ADMIN** — full access to all resources
                        - **COACH** — manages learning paths, adds references, creates notes
                        - **STUDENT** — manages own notes and learning paths

                        ## Note
                        Swagger UI is enabled in all environments for this project.
                        In a production deployment this would be restricted to dev/test only.
                        """)
                .contact(new Contact()
                        .name("NotePlus")
                        .email("admin@noteplus.nl"))
                .license(new License()
                        .name("School project — Novi Hogeschool")
                        .url("https://www.novi-hogeschool.nl"));
    }
}
```

## Step 4 — Verify AuthController has no @SecurityRequirement

`AuthController` endpoints (`/api/auth/register` and `/api/auth/login`) are PUBLIC.
They must NOT have `@SecurityRequirement(name = "bearerAuth")` — that would show
a padlock icon in Swagger suggesting they need a token, which is misleading and incorrect.

Read `AuthController.java`. If `@SecurityRequirement` is present on the class or methods, remove it.
The class-level `@Tag` annotation is fine to keep.

## Step 5 — Verify
Open Swagger UI: http://localhost:8080/swagger-ui/index.html

Verify the following checklist manually:

**UI checks:**
- [ ] "Authorize" button (🔒) appears at the top right of the Swagger UI page
- [ ] Clicking Authorize shows a dialog with "bearerAuth (http, Bearer)" label
- [ ] The dialog explains to paste the JWT token
- [ ] API title shows "NotePlus API" with version "1.0.0"
- [ ] All controller groups appear (Authentication, Notes, Categories, Learning Paths, References)
- [ ] A search/filter bar appears at the top of the endpoint list

**Security checks:**
- [ ] `/api/auth/register` and `/api/auth/login` show NO padlock icon
- [ ] `/api/notes`, `/api/categories`, `/api/references` etc. show a padlock icon (🔒)
- [ ] GET /api/notes without authorizing returns 401
- [ ] After clicking Authorize and pasting a valid token, GET /api/notes returns 200

**Token flow test:**
1. POST /api/auth/login → copy the `token` value from the response
2. Click Authorize → paste the token (without "Bearer " prefix) → click Authorize
3. Close the dialog
4. GET /api/notes → should return 200 with your notes
5. Click Authorize → click Logout → GET /api/notes → should return 401 again

If the Authorize button does not appear:
- Verify the bean method name is unique (no other `OpenAPI` bean in the project)
- Verify `springdoc-openapi-starter-webmvc-ui` is in pom.xml
- Check for startup errors mentioning `springdoc` or `OpenApiConfig` in the logs

Do NOT proceed until all checklist items above are confirmed.
