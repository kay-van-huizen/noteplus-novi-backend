Build the JwtAuthenticationFilter for NotePlus.

Read CLAUDE.md. Read JwtService and CustomUserDetailsService that were just built.

Create `src/main/java/org/noteplus/noteplus/security/JwtAuthenticationFilter.java`:

- Extends OncePerRequestFilter
- @Component @RequiredArgsConstructor
- Inject JwtService and CustomUserDetailsService

Implement doFilterInternal:
1. Read the Authorization header
2. If header is null or does not start with "Bearer ", call filterChain.doFilter and return immediately
3. Extract the token (substring after "Bearer ")
4. Wrap everything in try-catch. On ANY exception (expired, malformed, signature invalid):
    - Set response status to 401
    - Set content type to application/json
    - Write {"error": "Invalid or expired token"} to response
    - Return — do NOT continue the filter chain
5. Extract username from token via JwtService
6. If username is not null AND SecurityContextHolder has no existing authentication:
    - Load UserDetails via CustomUserDetailsService
    - Call jwtService.isTokenValid(token, userDetails)
    - If valid: create UsernamePasswordAuthenticationToken with userDetails + authorities + the request as details (WebAuthenticationDetailsSource)
    - Set it in SecurityContextHolder.getContext().setAuthentication(...)
7. Call filterChain.doFilter(request, response)
