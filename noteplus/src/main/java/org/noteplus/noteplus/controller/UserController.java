package org.noteplus.noteplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.ChangePasswordRequest;
import org.noteplus.noteplus.dto.response.MeResponse;
import org.noteplus.noteplus.dto.response.UserResponse;
import org.noteplus.noteplus.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User account management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user's profile")
    @ApiResponse(responseCode = "200", description = "User profile returned")
    public ResponseEntity<MeResponse> getMe(Authentication auth) {
        return ResponseEntity.ok(userService.getCurrentUser(auth.getName()));
    }

    @GetMapping("/coaches")
    @Operation(summary = "Get all users with COACH role")
    @ApiResponse(responseCode = "200", description = "Coaches retrieved")
    public ResponseEntity<List<UserResponse>> getCoaches() {
        return ResponseEntity.ok(userService.getUsersByRole("ROLE_COACH"));
    }

    @GetMapping("/students")
    @Operation(summary = "Get all users with STUDENT role")
    @ApiResponse(responseCode = "200", description = "Students retrieved")
    public ResponseEntity<List<UserResponse>> getStudents() {
        return ResponseEntity.ok(userService.getUsersByRole("ROLE_STUDENT"));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change password for the currently authenticated user")
    @ApiResponse(responseCode = "204", description = "Password changed successfully")
    @ApiResponse(responseCode = "400", description = "Passwords do not match or same as current")
    @ApiResponse(responseCode = "403", description = "Current password is incorrect")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication auth) {
        userService.changePassword(request, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
