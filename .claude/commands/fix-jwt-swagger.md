# Fix — JWT Token "Invalid or expired token" in Swagger

## What's wrong

Two issues combined to break token validation:

**1. Filter runs twice (double registration)**
`JwtAuthenticationFilter` is annotated with `@Component`, which causes Spring Boot to
register it in the Servlet container filter chain. `SecurityConfig` also registers it
via `addFilterBefore`. The filter runs twice per request. `OncePerRequestFilter` prevents
double execution of the logic, but the interaction with Spring Security's stateless
`SecurityContextHolder` clearing between the two chains can produce unexpected behavior.

**2. Silent exception — no visibility into the real cause**
The catch block wrote `{"error": "Invalid or expired token"}` without logging anything.
You could not see the actual exception class or message. Adding `@Slf4j` + `log.error`
makes the real error visible in the IntelliJ console immediately.

**3. Key derivation (safety fix)**
`Decoders.BASE64.decode(secret)` works correctly for this secret, but switching to
`secret.getBytes(UTF_8)` is simpler and eliminates any possible Base64 edge case.
The new secret (below) is 128 hex chars = 128 bytes UTF-8 = 1024 bits, well above
the 512-bit minimum for HS512.

---

## Files to replace

### 1. Replace `JwtAuthenticationFilter.java`
Path: `src/main/java/org/noteplus/noteplus/security/JwtAuthenticationFilter.java`

Changes:
- Added `@Slf4j` + `log.error(...)` in catch block — **you will now see the real exception in IntelliJ console**
- Added `.trim()` to `authHeader.substring(7)` — strips accidental whitespace from the token

### 2. Replace `JwtService.java`
Path: `src/main/java/org/noteplus/noteplus/security/JwtService.java`

Changes:
- `signWith(signingKey(), Jwts.SIG.HS512)` — explicit algorithm, no auto-detection
- `signingKey()` now uses `secret.getBytes(StandardCharsets.UTF_8)` instead of `Decoders.BASE64.decode()`
- Removed the `io.jsonwebtoken.io.Decoders` import (no longer needed)

### 3. Replace `SecurityConfig.java`
Path: `src/main/java/org/noteplus/noteplus/security/SecurityConfig.java`

Changes:
- Added `FilterRegistrationBean<JwtAuthenticationFilter>` bean — prevents double registration
- Added `http://localhost:8080` to CORS allowed origins (needed for Swagger UI same-origin requests)

### 4. Update `application.properties`

Replace the `app.jwt.secret` line with:
```properties
app.jwt.secret=9888117e5f57c059f14d1bc002868b3ecf8387bd5c2e63a1f4d7792248c0add375e8f2ccb1cc3d3f17a7644c5f9d4bc27b49ae51a4496a86e3692675bc9ac504
```

This is a 128-char hex string = 128 UTF-8 bytes = 1024-bit key. Safe for HS512.

**Important:** changing the secret invalidates all existing tokens. After restart, log in again to get new tokens.

---

## Steps

1. Copy the 3 `.java` files from the output folder into your project (replace existing)
2. Update `app.jwt.secret` in `application.properties` (copy the value above)
3. Restart the app: `./mvnw spring-boot:run`
4. Watch the IntelliJ console — if something still fails, you will now see the exact exception name and message
5. Log in fresh via `POST /api/auth/login` with `admin` / `Test1234!`
6. Copy the new token → Authorize in Swagger → test `GET /api/categories`

---

## If it still fails after these changes

The log will now print the real exception. Look for a line like:

```
JWT validation failed for path [/api/categories]: SignatureException — JWT signature does not match...
JWT validation failed for path [/api/categories]: MalformedJwtException — ...
JWT validation failed for path [/api/categories]: IllegalArgumentException — ...
```

Paste that line here and we can fix it immediately.

---

## Smoke test table (after fix)

| # | Method | Endpoint | Token | Expected |
|---|--------|----------|-------|----------|
| 1 | POST | `/api/auth/login` | — | 200 + token |
| 2 | GET | `/api/categories` | student1 | 200 |
| 3 | POST | `/api/categories` | student1 | 403 |
| 4 | POST | `/api/categories` | coach1 | 201 |
| 5 | DELETE | `/api/categories/{id}` | coach1 | 403 |
| 6 | DELETE | `/api/categories/{id}` | admin | 204 |
| 7 | GET | `/api/notes` | student1 | 200 |
| 8 | GET | `/api/notes/all` | student1 | 403 |
| 9 | GET | `/api/notes/all` | admin | 200 |
