Build the complete authentication feature for NotePlus: DTOs, AuthService, AuthController.

Read CLAUDE.md. Read SecurityConfig, JwtService, UserRepository, and the User + Role entities.

Step A — Create DTOs in src/main/java/org/noteplus/noteplus/dto/request/:

RegisterRequest.java (Java record):
- String username — @NotBlank @Size(min=3, max=50)
- String email — @NotBlank @Email
- String password — @NotBlank @Size(min=8, message="Password must be at least 8 characters")

LoginRequest.java (Java record):
- String username — @NotBlank
- String password — @NotBlank

Create src/main/java/org/noteplus/noteplus/dto/response/AuthResponse.java (Java record):
- String token
- String username
- List<String> roles

Step B — Create src/main/java/org/noteplus/noteplus/service/AuthService.java (interface):
- AuthResponse register(RegisterRequest request)
- AuthResponse login(LoginRequest request)

Step C — Create src/main/java/org/noteplus/noteplus/service/impl/AuthServiceImpl.java:
- @Service @RequiredArgsConstructor
- Inject UserRepository, PasswordEncoder, JwtService, AuthenticationManager, RoleRepository (create if needed)
- register():
    - Check existsByUsername — throw DuplicateResourceException("Username already taken") if true
    - Check existsByEmail — throw DuplicateResourceException("Email already registered") if true
    - Build new User entity, encode password with passwordEncoder
    - Assign default role ROLE_STUDENT (fetch from RoleRepository by name)
    - Save user
    - Generate token via JwtService
    - Return AuthResponse
- login():
    - Call authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password))
    - This throws BadCredentialsException automatically if wrong — catch it and throw new AccessDeniedException("Invalid credentials")
    - Load UserDetails, generate token, return AuthResponse

Step D — Create src/main/java/org/noteplus/noteplus/controller/AuthController.java:
- @RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
- @Tag(name = "Authentication")
- POST /register — @Valid body, returns 201 Created
- POST /login — @Valid body, returns 200 OK
- No @PreAuthorize — these are public endpoints
