package org.noteplus.noteplus.service.impl;

import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.LoginRequest;
import org.noteplus.noteplus.dto.request.RegisterRequest;
import org.noteplus.noteplus.dto.response.AuthResponse;
import org.noteplus.noteplus.entity.User;
import org.noteplus.noteplus.exception.AccessDeniedException;
import org.noteplus.noteplus.exception.DuplicateResourceException;
import org.noteplus.noteplus.repository.RoleRepository;
import org.noteplus.noteplus.repository.UserRepository;
import org.noteplus.noteplus.security.JwtService;
import org.noteplus.noteplus.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered");
        }

        var defaultRole = roleRepository.findByName("ROLE_STUDENT")
                .orElseThrow(() -> new IllegalStateException("Default role ROLE_STUDENT not found in database"));

        var user = new User();
        user.setName(request.username());
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(defaultRole));

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        return new AuthResponse(token, user.getUsername(), roles);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException ex) {
            throw new AccessDeniedException("Invalid credentials");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        String token = jwtService.generateToken(userDetails);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        return new AuthResponse(token, request.username(), roles);
    }
}