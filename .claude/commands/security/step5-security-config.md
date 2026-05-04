Build the SecurityConfig for NotePlus Spring Boot 4 / Spring Security 6.

Read CLAUDE.md. Read all security classes built so far (JwtFilter, CustomUserDetailsService, JwtService).

Create `src/main/java/org/noteplus/noteplus/security/SecurityConfig.java`:

- @Configuration @EnableWebSecurity @EnableMethodSecurity @RequiredArgsConstructor
- Inject JwtAuthenticationFilter and CustomUserDetailsService

Expose these beans:

1. SecurityFilterChain filterChain(HttpSecurity http):
    - Disable CSRF (stateless API, no browser sessions)
    - Configure CORS: allow http://localhost:3000 and http://localhost:5173 for dev, all methods, all headers
    - SessionManagement: STATELESS
    - ExceptionHandling: set authenticationEntryPoint that returns 401 JSON (not redirect to /login)
    - Permit without auth: /api/auth/**, /swagger-ui/**, /v3/api-docs/**, /error
    - All other requests: authenticated()
    - Add JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter

2. PasswordEncoder passwordEncoder():
    - Return new BCryptPasswordEncoder()

3. AuthenticationProvider authenticationProvider():
    - DaoAuthenticationProvider
    - Set CustomUserDetailsService as userDetailsService
    - Set BCryptPasswordEncoder as passwordEncoder

4. AuthenticationManager authenticationManager(AuthenticationConfiguration config):
    - Return config.getAuthenticationManager()

Critical checks:
- The authenticationEntryPoint must return JSON, not HTML — write {"error": "Unauthorized"} directly to response
- Do NOT use anyRequest().permitAll() — that defeats the purpose
- @EnableMethodSecurity is required for @PreAuthorize to work on controllers
