Build the JwtProperties configuration class for the NotePlus Spring Boot project.

Read CLAUDE.md for architecture rules first.

Create `src/main/java/org/noteplus/noteplus/security/JwtProperties.java`:

- Annotate with @ConfigurationProperties(prefix = "app.jwt")
- Add @Component
- Two fields: `secret` (String) and `expirationMs` (long)
- Use Lombok @Getter @Setter

Then update `src/main/resources/application.properties`:
- Add `app.jwt.secret=` with a placeholder comment: # Replace with: openssl rand -base64 64
- Add `app.jwt.expiration-ms=86400000` (24 hours)
- Add `spring.jpa.hibernate.ddl-auto=update`
- Add `spring.sql.init.mode=never` (we use data.sql later, disable for now)
