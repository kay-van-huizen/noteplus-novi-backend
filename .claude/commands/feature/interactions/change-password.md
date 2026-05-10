---
description: Builds the change password feature — backend endpoint, service logic with BCrypt verification, and a settings JTE page accessible when logged in. Run after auth-frontend.md is complete.
allowed-tools: Read, Write, Bash(git checkout:*), Bash(git pull:*), Bash(./mvnw compile:*), Glob, Grep
---

# Change Password — NotePlus

## What this feature does

An authenticated user can change their password from a settings page.
They must provide their current password (verified with BCrypt) and a new password
(entered twice to prevent typos). Only then is the password updated.

## Security rules

- Current password MUST be verified with BCrypt before accepting the new one
- `newPassword` and `confirmPassword` must match — validated in the service, not just the DTO
- Minimum password length: 8 characters (same rule as registration)
- After a successful password change, the existing JWT remains valid until expiry
  (acceptable for school project — in production you would invalidate all tokens)
- The endpoint requires authentication — anonymous users cannot access it

## Step 0 — Read context first (MANDATORY)

```
src/main/java/org/noteplus/noteplus/entity/User.java
src/main/java/org/noteplus/noteplus/repository/UserRepository.java
src/main/java/org/noteplus/noteplus/security/SecurityConfig.java
src/main/java/org/noteplus/noteplus/service/AuthService.java          ← interface pattern
src/main/java/org/noteplus/noteplus/service/impl/AuthServiceImpl.java ← impl pattern
src/main/java/org/noteplus/noteplus/exception/ForbiddenException.java
src/main/java/org/noteplus/noteplus/exception/ResourceNotFoundException.java
src/main/jte/auth/login.jte                                            ← style reference
CLAUDE.md
```

## Step 2 — DTO

`src/main/java/org/noteplus/noteplus/dto/request/ChangePasswordRequest.java`:

```java
package org.noteplus.noteplus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "Current password is required")
    String currentPassword,

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    String newPassword,

    @NotBlank(message = "Please confirm your new password")
    String confirmPassword
) {}
```

## Step 3 — UserService interface

Check if a `UserService` interface already exists.
If not, create `src/main/java/org/noteplus/noteplus/service/UserService.java`:

```java
package org.noteplus.noteplus.service;

import org.noteplus.noteplus.dto.request.ChangePasswordRequest;

public interface UserService {
    void changePassword(ChangePasswordRequest request, String username);
}
```

## Step 4 — UserServiceImpl

`src/main/java/org/noteplus/noteplus/service/impl/UserServiceImpl.java`:

- `@Service @RequiredArgsConstructor`
- Inject: `UserRepository`, `PasswordEncoder`

```java
package org.noteplus.noteplus.service.impl;

import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.ChangePasswordRequest;
import org.noteplus.noteplus.exception.ForbiddenException;
import org.noteplus.noteplus.exception.ResourceNotFoundException;
import org.noteplus.noteplus.repository.UserRepository;
import org.noteplus.noteplus.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void changePassword(ChangePasswordRequest request, String username) {

        // 1. Load user
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Verify current password with BCrypt
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ForbiddenException("Current password is incorrect");
        }

        // 3. Confirm new passwords match — validate in service, not just DTO
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }

        // 4. Prevent setting the same password again
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        // 5. Encode and save
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
```

## Step 5 — REST endpoint

Add a `UserController` or add to an existing one if it already exists.

`src/main/java/org/noteplus/noteplus/controller/UserController.java`:

```java
package org.noteplus.noteplus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.ChangePasswordRequest;
import org.noteplus.noteplus.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User account management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

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
```

## Step 6 — GlobalExceptionHandler: add IllegalArgumentException handler

Read `GlobalExceptionHandler.java`.
Add a handler for `IllegalArgumentException` if not already present:

```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(400)
        .body(new ErrorResponse(400, "Bad Request", ex.getMessage(), LocalDateTime.now()));
}
```

## Step 7 — Settings ViewController

`src/main/java/org/noteplus/noteplus/controller/SettingsViewController.java`:

```java
package org.noteplus.noteplus.controller;

import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.ChangePasswordRequest;
import org.noteplus.noteplus.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsViewController {

    private final UserService userService;

    @GetMapping
    public String showSettings(Model model) {
        model.addAttribute("error", null);
        model.addAttribute("success", null);
        return "settings/index";
    }

    @PostMapping("/password")
    public String handleChangePassword(
            @ModelAttribute ChangePasswordRequest request,
            Authentication auth,
            Model model) {
        try {
            userService.changePassword(request, auth.getName());
            model.addAttribute("success", "Password changed successfully.");
            model.addAttribute("error", null);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("success", null);
        }
        return "settings/index";
    }
}
```

Add `/settings/**` to SecurityConfig permitAll() only if you want unauthenticated access.
Settings is a protected page — it should require login. The JWT cookie handles this automatically
as long as the JwtAuthenticationFilter is updated (from auth-frontend.md).

If the user is not logged in and visits /settings, they will get a 401 and be redirected.
Add this to SecurityConfig if you want a proper redirect to /login:

```java
.exceptionHandling(ex -> ex
    .authenticationEntryPoint((request, response, e) -> {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("text/html")) {
            response.sendRedirect("/login");
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized\"}");
        }
    })
)
```

This redirects browser requests to /login and returns JSON 401 for API clients.

## Step 8 — settings/index.jte

Create `src/main/jte/settings/`.

`src/main/jte/settings/index.jte`:

```html
@param String error
@param String success

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Settings — NotePlus</title>
    <style>
        * { box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            background: #f5f5f5;
            margin: 0;
            padding: 40px 16px;
        }
        .container { max-width: 480px; margin: 0 auto; }
        h1 { font-size: 22px; color: #1a1a1a; margin-bottom: 24px; }
        .card {
            background: white;
            padding: 28px 32px;
            border-radius: 8px;
            box-shadow: 0 1px 4px rgba(0,0,0,0.1);
        }
        h2 { font-size: 16px; margin: 0 0 18px; color: #374151; }
        label {
            display: block;
            font-size: 13px;
            font-weight: 600;
            color: #555;
            margin-top: 14px;
            margin-bottom: 3px;
        }
        input {
            width: 100%;
            padding: 9px 12px;
            border: 1px solid #d1d5db;
            border-radius: 6px;
            font-size: 14px;
        }
        input:focus {
            outline: none;
            border-color: #2563eb;
            box-shadow: 0 0 0 2px rgba(37,99,235,0.15);
        }
        .error {
            background: #fef2f2;
            color: #dc2626;
            padding: 10px 14px;
            border-radius: 6px;
            font-size: 13px;
            margin-bottom: 14px;
        }
        .success {
            background: #f0fdf4;
            color: #16a34a;
            padding: 10px 14px;
            border-radius: 6px;
            font-size: 13px;
            margin-bottom: 14px;
        }
        .hint { font-size: 11px; color: #9ca3af; margin-top: 3px; }
        .actions { display: flex; gap: 10px; margin-top: 22px; }
        .btn-primary {
            padding: 10px 22px;
            background: #2563eb;
            color: white;
            border: none;
            border-radius: 6px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
        }
        .btn-primary:hover { background: #1d4ed8; }
        a.back { font-size: 13px; color: #6b7280; text-decoration: none; line-height: 2.4; }
    </style>
</head>
<body>
<div class="container">
    <h1>Settings</h1>

    <div class="card">
        <h2>Change Password</h2>

        @if(error != null)
            <div class="error">${error}</div>
        @endif

        @if(success != null)
            <div class="success">${success}</div>
        @endif

        <form method="post" action="/settings/password">
            <label for="currentPassword">Current password</label>
            <input type="password" id="currentPassword" name="currentPassword"
                   required autocomplete="current-password">

            <label for="newPassword">New password</label>
            <input type="password" id="newPassword" name="newPassword"
                   required minlength="8" autocomplete="new-password">
            <p class="hint">At least 8 characters</p>

            <label for="confirmPassword">Confirm new password</label>
            <input type="password" id="confirmPassword" name="confirmPassword"
                   required minlength="8" autocomplete="new-password">

            <div class="actions">
                <button class="btn-primary" type="submit">Change Password</button>
                <a class="back" href="/notes">← Back</a>
            </div>
        </form>
    </div>
</div>
</body>
</html>
```

## Step 9 — Test

| Test | Expected |
|------|----------|
| GET /settings (logged in) | Settings page renders |
| GET /settings (not logged in) | Redirect to /login |
| POST /settings/password — correct current, matching new | "Password changed successfully." |
| POST /settings/password — wrong current password | "Current password is incorrect" |
| POST /settings/password — new passwords don't match | "New passwords do not match" |
| POST /settings/password — same as current | "New password must be different" |
| POST /settings/password — new < 8 chars | "New password must be at least 8 characters" |
| PUT /api/users/me/password via Swagger | 204 No Content |
