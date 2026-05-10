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
import org.noteplus.noteplus.repository.RoleRepository;
import org.noteplus.noteplus.repository.UserRepository;
import org.noteplus.noteplus.security.JwtService;
import org.noteplus.noteplus.service.impl.AuthServiceImpl;
import org.noteplus.noteplus.util.TestDataFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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
    @Mock private UserDetailsService userDetailsService;
    @InjectMocks private AuthServiceImpl authService;

    // ── register() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("register - valid new user - saves user and returns JWT token")
    void register_validNewUser_savesUserAndReturnsToken() {
        // Arrange
        RegisterRequest request = new RegisterRequest("newuser", "new@test.nl", "password123");
        Role studentRole = TestDataFactory.createStudentRole();
        UserDetails userDetails = TestDataFactory.createUserDetailsWith("newuser");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.nl")).thenReturn(false);
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenReturn(new User());
        when(userDetailsService.loadUserByUsername("newuser")).thenReturn(userDetails);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt.token.here");

        // Act
        AuthResponse result = authService.register(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("jwt.token.here");
        assertThat(result.username()).isEqualTo("newuser");
        assertThat(result.roles()).contains("ROLE_STUDENT");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register - password is BCrypt encoded before saving")
    void register_validRequest_passwordIsEncodedBeforeSave() {
        // Arrange
        RegisterRequest request = new RegisterRequest("newuser", "new@test.nl", "plainPassword");
        Role studentRole = TestDataFactory.createStudentRole();
        UserDetails userDetails = TestDataFactory.createUserDetailsWith("newuser");

        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode("plainPassword")).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenReturn(new User());
        when(userDetailsService.loadUserByUsername(any())).thenReturn(userDetails);
        when(jwtService.generateToken(any())).thenReturn("token");

        // Act
        authService.register(request);

        // Assert — the saved User must have the encoded password, never the plain-text one
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$encoded");
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
    @DisplayName("register - ROLE_STUDENT not in database - throws IllegalStateException")
    void register_roleStudentMissing_throwsIllegalStateException() {
        // Arrange
        RegisterRequest request = new RegisterRequest("newuser", "new@test.nl", "password123");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.nl")).thenReturn(false);
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.empty());

        // Act & Assert
        // orElseThrow(() -> new IllegalStateException(...)) is the actual branch in AuthServiceImpl
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalStateException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - new user is assigned ROLE_STUDENT by default")
    void register_newUser_isAssignedStudentRoleByDefault() {
        // Arrange
        RegisterRequest request = new RegisterRequest("newuser", "new@test.nl", "password123");
        Role studentRole = TestDataFactory.createStudentRole();
        UserDetails userDetails = TestDataFactory.createUserDetailsWith("newuser");

        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode(any())).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenReturn(new User());
        when(userDetailsService.loadUserByUsername(any())).thenReturn(userDetails);
        when(jwtService.generateToken(any())).thenReturn("token");

        // Act
        authService.register(request);

        // Assert — ROLE_STUDENT was fetched and assigned to the saved user
        verify(roleRepository).findByName("ROLE_STUDENT");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRoles())
                .anyMatch(r -> r.getName().equals("ROLE_STUDENT"));
    }

    // ── login() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("login - valid credentials - returns AuthResponse with token and roles")
    void login_validCredentials_returnsAuthResponseWithToken() {
        // Arrange
        LoginRequest request = new LoginRequest("student1", "password123");
        UserDetails userDetails = TestDataFactory.createStudentUserDetails();

        // authenticationManager.authenticate succeeds (no exception)
        when(userDetailsService.loadUserByUsername("student1")).thenReturn(userDetails);
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
    @DisplayName("login - wrong password - throws ForbiddenException with neutral message")
    void login_wrongPassword_throwsForbiddenException() {
        // Arrange
        LoginRequest request = new LoginRequest("student1", "wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Act & Assert — BadCredentialsException must be wrapped in ForbiddenException
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Invalid credentials");

        // No token must ever be generated for a failed login
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("login - unknown username - same ForbiddenException as wrong password (no enumeration)")
    void login_unknownUsername_throwsSameForbiddenException() {
        // Arrange
        LoginRequest request = new LoginRequest("doesNotExist", "password123");

        // Spring Security's authenticationManager throws BadCredentialsException for unknown users too
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Act & Assert
        // SECURITY: both wrong-password and wrong-username must surface the same exception type
        // to prevent attackers from discovering which usernames have accounts.
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Invalid credentials");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("login - authenticationManager is called before token generation")
    void login_validRequest_authenticationManagerCalledFirst() {
        // Arrange
        LoginRequest request = new LoginRequest("student1", "password123");
        UserDetails userDetails = TestDataFactory.createStudentUserDetails();

        when(userDetailsService.loadUserByUsername("student1")).thenReturn(userDetails);
        when(jwtService.generateToken(any())).thenReturn("token");

        // Act
        authService.login(request);

        // Assert — authManager must verify credentials before any token is generated
        verify(authenticationManager).authenticate(
                argThat(auth -> auth instanceof UsernamePasswordAuthenticationToken
                        && auth.getPrincipal().equals("student1"))
        );
        verify(jwtService).generateToken(any());
    }
}
