Build the JwtService for the NotePlus Spring Boot project using jjwt 0.12.6.

Read CLAUDE.md first. Read the JwtProperties class that was just created.

Create `src/main/java/org/noteplus/noteplus/security/JwtService.java`:

- @Service @RequiredArgsConstructor
- Inject JwtProperties
- Implement these methods:

  String generateToken(UserDetails userDetails)
    - Claims: subject = username, roles = list of authority names, issuedAt = now, expiration = now + expirationMs
    - Sign with HMAC-SHA256 using the secret from JwtProperties
    - Use Jwts.builder() from jjwt 0.12.6 API (use hmacShaKeyFor pattern)

  String extractUsername(String token)
    - Extract subject claim

  boolean isTokenValid(String token, UserDetails userDetails)
    - Check: extractUsername matches userDetails.getUsername() AND token is not expired

  private Claims extractAllClaims(String token)
    - Use Jwts.parserBuilder() with the signing key

  private boolean isTokenExpired(String token)
    - Check expiration claim against now

Security rules to enforce:
- The signing key must be derived from the secret string as a SecretKey (use Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
- NEVER put the raw secret string in the token or logs
- No sensitive user data in claims — only username and roles
