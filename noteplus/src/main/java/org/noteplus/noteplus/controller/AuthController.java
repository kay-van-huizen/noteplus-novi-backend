package org.noteplus.noteplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.ForgotPasswordRequest;
import org.noteplus.noteplus.dto.request.LoginRequest;
import org.noteplus.noteplus.dto.request.RegisterRequest;
import org.noteplus.noteplus.dto.request.ResetPasswordRequest;
import org.noteplus.noteplus.dto.response.AuthResponse;
import org.noteplus.noteplus.security.JwtProperties;
import org.noteplus.noteplus.service.AuthService;
import org.noteplus.noteplus.service.PasswordResetService;
import org.noteplus.noteplus.util.CookieUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login endpoints")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final CookieUtil cookieUtil;
    private final JwtProperties jwtProperties;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "409", description = "Username or email already taken")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletResponse response) {
        AuthResponse auth = authService.register(request);
        cookieUtil.addJwtCookie(response, auth.token(), jwtProperties.getExpirationMs() / 1000);
        return ResponseEntity.status(HttpStatus.CREATED).body(auth);
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive a JWT token")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletResponse response) {
        AuthResponse auth = authService.login(request);
        cookieUtil.addJwtCookie(response, auth.token(), jwtProperties.getExpirationMs() / 1000);
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and clear the JWT cookie")
    @ApiResponse(responseCode = "200", description = "Logged out successfully")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        cookieUtil.clearJwtCookie(response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset email")
    @ApiResponse(responseCode = "200", description = "If the email is registered, a reset link has been sent")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        // SECURITY: always return same message regardless of whether email exists
        return ResponseEntity.ok("If this email is registered, you will receive a reset link.");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using a token from the reset email")
    @ApiResponse(responseCode = "204", description = "Password reset successfully")
    @ApiResponse(responseCode = "400", description = "Invalid token, expired, or passwords do not match")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword(), request.confirmPassword());
        return ResponseEntity.noContent().build();
    }
}