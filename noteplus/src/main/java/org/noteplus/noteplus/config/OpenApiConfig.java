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
        // Security scheme — defines what "bearerAuth" means.
        // This is what @SecurityRequirement(name = "bearerAuth") in each controller refers to.
        SecurityScheme bearerScheme = new SecurityScheme()
                .name(BEARER_AUTH)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste your JWT token here. Obtain it from POST /api/auth/login.");

        // Global security requirement — applies bearerAuth to all endpoints by default.
        // Public endpoints (AuthController) have no @SecurityRequirement, so they show no padlock.
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
                        .url("https://www.novi.nl/"));
    }
}
