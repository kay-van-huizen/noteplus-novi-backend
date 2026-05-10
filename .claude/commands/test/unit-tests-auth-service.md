---
description: Writes complete unit tests for AuthServiceImpl targeting 100% line coverage. Uses JUnit 5 + Mockito. School requirement: 2 service classes at 100% coverage, minimum 10 unit tests total using Arrange-Act-Assert.
allowed-tools: Read, Write, Bash(./mvnw test:*), Glob, Grep
---

# Unit Tests — AuthServiceImpl

## School requirements covered by this file

- ✅ 2 of 2 required service classes at 100% line coverage
- ✅ Combined with NoteServiceImpl tests: 14+ unit tests total
- ✅ Arrange → Act → Assert on every test
- ✅ Own test data via TestDataFactory

## Step 0 — Read context first (MANDATORY)

Before writing any test code, read:

```
src/main/java/org/noteplus/noteplus/service/impl/AuthServiceImpl.java   ← every branch
src/main/java/org/noteplus/noteplus/service/AuthService.java
src/main/java/org/noteplus/noteplus/entity/User.java
src/main/java/org/noteplus/noteplus/entity/Role.java
src/main/java/org/noteplus/noteplus/repository/UserRepository.java
src/main/java/org/noteplus/noteplus/repository/RoleRepository.java
src/main/java/org/noteplus/noteplus/security/JwtService.java
src/main/java/org/noteplus/noteplus/dto/request/RegisterRequest.java
src/main/java/org/noteplus/noteplus/dto/request/LoginRequest.java
src/main/java/org/noteplus/noteplus/dto/response/AuthResponse.java
src/main/java/org/noteplus/noteplus/exception/DuplicateResourceException.java
src/main/java/org/noteplus/noteplus/exception/ForbiddenException.java
src/test/java/org/noteplus/noteplus/util/TestDataFactory.java           ← must exist from note tests
```

Map every branch before writing tests.
AuthServiceImpl has these critical paths:
- register: username exists → exception
- register: email exists → exception
- register: ROLE_STUDENT not found → exception
- register: happy path → saves user with encoded password + returns token
- login: bad credentials → exception
- login: happy path → returns token with correct username and roles

## Step 1 — Verify TestDataFactory exists

Check if `src/test/java/org/noteplus/noteplus/util/TestDataFactory.java` exists.
If not, create it first using the content from unit-tests-note-service.md.

## Step 2 — Write AuthServiceImplTest

Create `src/test/java/org/noteplus/noteplus/service/AuthServiceImplTest.java`:

```java
package org.noteplus.noteplus.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.noteplus.noteplus.dto.request.LoginRequest;
import org.noteplus.noteplus.dto.request.RegisterRequest;
import org.noteplus.noteplus.dto.response.AuthResponse;
import org.noteplus.noteplus.entity.Role;
import org.noteplus.noteplus.entity.User;
import org.noteplus.noteplus.exception.DuplicateResourceException;
import org.noteplus.noteplus.exception.ForbiddenException;
import org.noteplus.noteplus.exception.ResourceNotFoundException;
import org.noteplus.noteplus.repository.RoleRepository;
import org.noteplus.noteplus.repository.UserRepository;
import org.noteplus.noteplus.security.JwtService;
import org.noteplus.noteplus.service.impl.AuthServiceImpl;
import org.noteplus.noteplus.util.TestDataFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.
 * Target: 100% line coverage.
 * Pattern: Arrange → Act → Assert on every test.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @InjectMocks private AuthServiceImpl authService;

    // ── register() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("register - valid new user - saves user and returns JWT token")
    void register_validNewUser_savesUserAndReturnsToken() {
        // Arrange
        RegisterRequest request = new RegisterRequest("newuser", "new@test.nl", "password123");
        Role studentRole = TestDataFactory.createStudentRole();
        User savedUser = TestDataFactory.createStudent();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.nl")).thenReturn(false);
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt.token.here");

        // Act
        AuthResponse result = authService.register(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("jwt.token.here");
        assertThat(result.username()).isEqualTo("student1");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register - password is BCrypt encoded before saving")
    void register_validRequest_passwordIsEncodedBeforeSave() {
        // Arrange
        RegisterRequest request = new RegisterRequest("newuser", "new@test.nl", "plainPassword");
        Role studentRole = TestDataFactory.createStudentRole();
        User savedUser = TestDataFactory.createStudent();

        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode("plainPassword")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any())).thenReturn("token");

        // Act
        authService.register(request);

        // Assert — capture the saved user and verify the password was encoded
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$10$encoded");
        assertThat(captor.getValue().getPassword()).doesNotContain("plainPassword");
    }

    @Test
    @DisplayName("register - username already exists - throws DuplicateResourceException")
    void register_usernameAlreadyExists_throwsDuplicateResourceException() {
        // Arrange
        RegisterRequest request = new RegisterRequest("student1", "any@test.nl", "password123");
        when(userRepository.existsByUsername("student1")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - email already exists - throws DuplicateResourceException")
    void register_emailAlreadyExists_throwsDuplicateResourceException() {
        // Arrange
        RegisterRequest request = new RegisterRequest("newuser", "taken@test.nl", "password123");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("taken@test.nl")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - ROLE_STUDENT not in database - throws ResourceNotFoundException")
    void register_roleStudentMissing_throwsResourceNotFoundException() {
        // Arrange
        RegisterRequest request = new RegisterRequest("newuser", "new@test.nl", "password123");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.nl")).thenReturn(false);
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - new user is assigned ROLE_STUDENT by default")
    void register_newUser_isAssignedStudentRoleByDefault() {
        // Arrange
        RegisterRequest request = new RegisterRequest("newuser", "new@test.nl", "password123");
        Role studentRole = TestDataFactory.createStudentRole();
        User savedUser = TestDataFactory.createStudent();

        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any())).thenReturn("token");

        // Act
        authService.register(request);

        // Assert — verify ROLE_STUDENT was fetched and assigned
        verify(roleRepository).findByName("ROLE_STUDENT");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRoles())
                .anyMatch(r -> r.getName().equals("ROLE_STUDENT"));
    }

    // ── login() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("login - valid credentials - returns AuthResponse with token")
    void login_validCredentials_returnsAuthResponseWithToken() {
        // Arrange
        LoginRequest request = new LoginRequest("student1", "password123");
        User student = TestDataFactory.createStudent();
        student.setRoles(java.util.Set.of(TestDataFactory.createStudentRole()));

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt.token.here");

        // Act
        AuthResponse result = authService.login(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("jwt.token.here");
        assertThat(result.username()).isEqualTo("student1");
        assertThat(result.roles()).contains("ROLE_STUDENT");
    }

    @Test
    @DisplayName("login - wrong password - throws exception with neutral message")
    void login_wrongPassword_throwsExceptionWithNeutralMessage() {
        // Arrange
        LoginRequest request = new LoginRequest("student1", "wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(Exception.class);

        // Verify no token was generated for failed login
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("login - unknown username - throws exception")
    void login_unknownUsername_throwsException() {
        // Arrange
        LoginRequest request = new LoginRequest("unknown", "password123");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Act & Assert
        // SECURITY: same exception type regardless of whether user exists or password is wrong
        // This prevents username enumeration attacks
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(Exception.class);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("login - authenticationManager is always called before token generation")
    void login_validRequest_authenticationManagerCalledFirst() {
        // Arrange
        LoginRequest request = new LoginRequest("student1", "password123");
        User student = TestDataFactory.createStudent();
        student.setRoles(java.util.Set.of(TestDataFactory.createStudentRole()));

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(jwtService.generateToken(any())).thenReturn("token");

        // Act
        authService.login(request);

        // Assert — authManager must be called to verify credentials before anything else
        verify(authenticationManager).authenticate(
                argThat(auth -> auth instanceof UsernamePasswordAuthenticationToken
                        && auth.getPrincipal().equals("student1"))
        );
    }
}
```

## Step 3 — Run tests and check coverage

[//]: # ()
[//]: # (```bash)
[//]: # (./mvnw test -Dtest=AuthServiceImplTest)
[//]: # (```)
[//]: # ()
[//]: # (All tests must be GREEN before checking coverage.)
[//]: # ()
[//]: # (```bash)
[//]: # (./mvnw verify)
[//]: # (```)

Open `target/site/jacoco/index.html`.
Navigate to `AuthServiceImpl` and verify 100% line coverage.

If any line is RED, identify the uncovered branch and add a targeted test.
Common missed branches:
- The `else` path on username/email duplicate checks
- The role-not-found branch
- Exception wrapping in the login method

## Step 4 — Run both service test classes together

[//]: # (```bash)
[//]: # (./mvnw test -Dtest="NoteServiceImplTest,AuthServiceImplTest")
[//]: # (```)

Verify the combined output shows 14+ tests, all green.

## Step 5 — Checklist before committing

- [ ] All 8 tests in this file pass
- [ ] AuthServiceImpl shows 100% line coverage in JaCoCo report
- [ ] No `@SpringBootTest` — pure unit tests with Mockito only
- [ ] The "neutral message" test comment explains the security reason
- [ ] TestDataFactory is the only source of test data
- [ ] `verify(jwtService, never()).generateToken(any())` present on failed login tests
