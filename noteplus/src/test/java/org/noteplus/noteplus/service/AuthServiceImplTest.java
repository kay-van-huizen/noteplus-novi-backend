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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock UserDetailsService userDetailsService;
    @InjectMocks AuthServiceImpl authService;

    // ── register() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("register - valid new user - returns AuthResponse with token, username, and ROLE_STUDENT")
    void register_validRequest_returnsAuthResponseWithToken() {
        // Arrange
        var role = TestDataFactory.createStudentRole();
        var userDetails = TestDataFactory.createStudentUserDetails();
        when(userRepository.existsByUsername("student1")).thenReturn(false);
        when(userRepository.existsByEmail("student1@test.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userDetailsService.loadUserByUsername("student1")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        // Act
        AuthResponse result = authService.register(new RegisterRequest("student1", "student1@test.com", "password123"));

        // Assert
        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.username()).isEqualTo("student1");
        assertThat(result.roles()).contains("ROLE_STUDENT");
    }

    @Test
    @DisplayName("register - password is BCrypt-encoded before save - plain text never stored in DB")
    void register_encodesPasswordBeforeSave_plainTextNeverStored() {
        // Arrange
        var role = TestDataFactory.createStudentRole();
        var userDetails = TestDataFactory.createStudentUserDetails();
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("plainPassword")).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token");

        var captor = ArgumentCaptor.forClass(User.class);

        // Act
        authService.register(new RegisterRequest("student1", "student1@test.com", "plainPassword"));

        // Assert
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$encoded");
        assertThat(captor.getValue().getPassword()).doesNotContain("plainPassword");
    }

    @Test
    @DisplayName("register - username already exists - throws DuplicateResourceException with 'Username', save never called")
    void register_usernameAlreadyExists_throwsDuplicateResourceException() {
        // Arrange
        when(userRepository.existsByUsername("student1")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(new RegisterRequest("student1", "student1@test.com", "password123")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - email already exists - throws DuplicateResourceException with 'Email', save never called")
    void register_emailAlreadyExists_throwsDuplicateResourceException() {
        // Arrange
        when(userRepository.existsByUsername("student1")).thenReturn(false);
        when(userRepository.existsByEmail("student1@test.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(new RegisterRequest("student1", "student1@test.com", "password123")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - ROLE_STUDENT not in DB - throws IllegalStateException, save never called")
    void register_studentRoleNotFound_throwsIllegalStateException() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.register(new RegisterRequest("student1", "student1@test.com", "password123")))
                .isInstanceOf(IllegalStateException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - new user is assigned ROLE_STUDENT - roleRepository queried and saved user has that role")
    void register_assignsStudentRole_savedUserHasStudentRole() {
        // Arrange
        var role = TestDataFactory.createStudentRole();
        var userDetails = TestDataFactory.createStudentUserDetails();
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token");

        var captor = ArgumentCaptor.forClass(User.class);

        // Act
        authService.register(new RegisterRequest("student1", "student1@test.com", "password123"));

        // Assert
        verify(roleRepository).findByName("ROLE_STUDENT");
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRoles())
                .anyMatch(r -> r.getName().equals("ROLE_STUDENT"));
    }

    // ── login() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login - valid credentials - returns AuthResponse with token, username, and ROLE_STUDENT")
    void login_validCredentials_returnsAuthResponseWithToken() {
        // Arrange
        var userDetails = TestDataFactory.createStudentUserDetails();
        when(userDetailsService.loadUserByUsername("student1")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        // Act
        AuthResponse result = authService.login(new LoginRequest("student1", "password123"));

        // Assert
        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.username()).isEqualTo("student1");
        assertThat(result.roles()).contains("ROLE_STUDENT");
    }

    @Test
    @DisplayName("login - wrong password - authenticationManager throws BadCredentialsException, service throws ForbiddenException, token never generated")
    void login_wrongPassword_throwsForbiddenExceptionTokenNeverGenerated() {
        // Arrange
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(new LoginRequest("student1", "wrongPassword")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Invalid credentials");
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("login - unknown username - throws ForbiddenException (no user enumeration), token never generated")
    void login_unknownUsername_throwsForbiddenException() {
        // Arrange
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody", "password123")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Invalid credentials");
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("login - authenticationManager is called before token generation - verifies call order")
    void login_callOrder_authenticateBeforeTokenGeneration() {
        // Arrange
        var userDetails = TestDataFactory.createStudentUserDetails();
        when(userDetailsService.loadUserByUsername("student1")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");
        var order = inOrder(authenticationManager, jwtService);

        // Act
        authService.login(new LoginRequest("student1", "password123"));

        // Assert
        order.verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        order.verify(jwtService).generateToken(userDetails);
    }
}