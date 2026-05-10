---
description: Builds the forgot password feature — PasswordResetToken entity, email service with Spring Mail, secure token flow, reset endpoints, and JTE pages. This is the most complex auth feature and touches multiple layers.
allowed-tools: Read, Write, Bash(git checkout:*), Bash(git pull:*), Bash(./mvnw compile:*), Glob, Grep
---

# Forgot Password — NotePlus

## How the flow works

```
1. User visits /forgot-password → enters their email address
2. Backend checks if email exists in the database
3. If YES: generate a secure token → store hashed → send reset email with link
   If NO:  do nothing, but return the SAME response (prevents email enumeration)
4. User receives email with link: http://localhost:8080/reset-password?token=<raw token>
5. User enters new password + confirm on the reset page
6. Backend validates token (exists, not expired, not already used) → resets password
7. Token is marked as used → cannot be reused
```

## Security rules — read these before writing any code

**🔴 Never reveal whether an email address exists.**
Both "email found" and "email not found" return the same response:
"If this email is registered, you will receive a reset link."
This prevents an attacker from discovering which emails have accounts.

**🔴 Store the token as a SHA-256 hash, not plain text.**
The reset link contains the raw token. If the database is compromised, hashed tokens
cannot be used directly to reset passwords. This is the same principle as password hashing.

**🔴 Token must expire.**
Use a 1-hour expiry. After expiry, the token is rejected with "Reset link has expired."

**🔴 Token must be single-use.**
After a successful password reset, mark the token as `used = true`.
Any subsequent attempt with the same token must be rejected.

**🔴 Token must be cryptographically random.**
Use `SecureRandom` + hex encoding — not `UUID.randomUUID()`.
UUID v4 has 122 bits of randomness. SecureRandom with 32 bytes = 256 bits.

**🟡 For this school project: use Mailtrap for email testing.**
Mailtrap is a free fake SMTP inbox — it catches emails without actually sending them.
This means you do not need a real Gmail account or email provider.
Sign up at mailtrap.io and get the SMTP credentials for application.properties.

## Step 0 — Read context first (MANDATORY)

```
src/main/java/org/noteplus/noteplus/entity/BaseEntity.java
src/main/java/org/noteplus/noteplus/entity/User.java
src/main/java/org/noteplus/noteplus/repository/UserRepository.java
src/main/java/org/noteplus/noteplus/security/SecurityConfig.java
src/main/java/org/noteplus/noteplus/service/impl/AuthServiceImpl.java
src/main/java/org/noteplus/noteplus/exception/ResourceNotFoundException.java
src/main/jte/auth/login.jte                   ← style reference
src/main/resources/application.properties
pom.xml
CLAUDE.md
```


## Step 2 — Add Spring Mail dependency to pom.xml

Add inside `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

## Step 3 — Add mail config to application.properties

```properties
# Spring Mail — Mailtrap SMTP (replace with your Mailtrap credentials)
spring.mail.host=sandbox.smtp.mailtrap.io
spring.mail.port=2525
spring.mail.username=MAILTRAP_USERNAME
spring.mail.password=MAILTRAP_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Application base URL (used in reset email links)
app.base-url=http://localhost:8080

# Password reset token expiry in minutes
app.password-reset.expiry-minutes=60
```

These credentials are sensitive — application.properties is already gitignored. Good.

## Step 4 — PasswordResetToken entity

`src/main/java/org/noteplus/noteplus/entity/PasswordResetToken.java`:

```java
package org.noteplus.noteplus.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;   // SHA-256 hash of the raw token

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
```

Note: this entity does NOT extend BaseEntity — it has its own minimal lifecycle fields.

## Step 5 — PasswordResetTokenRepository

`src/main/java/org/noteplus/noteplus/repository/PasswordResetTokenRepository.java`:

```java
package org.noteplus.noteplus.repository;

import org.noteplus.noteplus.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    // Clean up expired tokens — call this periodically or before generating a new one
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now OR t.used = true")
    void deleteExpiredAndUsed(LocalDateTime now);
}
```

## Step 6 — TokenUtil helper

`src/main/java/org/noteplus/noteplus/util/TokenUtil.java`:

```java
package org.noteplus.noteplus.util;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Component
public class TokenUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Generate a cryptographically random 32-byte hex token (256 bits)
    public String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    // Hash the raw token with SHA-256 for safe database storage
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
```

## Step 7 — EmailService

`src/main/java/org/noteplus/noteplus/service/EmailService.java`:

```java
package org.noteplus.noteplus.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetLink);
}
```

`src/main/java/org/noteplus/noteplus/service/impl/EmailServiceImpl.java`:

```java
package org.noteplus.noteplus.service.impl;

import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Reset your NotePlus password");
        message.setText("""
            Hello,

            You requested to reset your NotePlus password.
            Click the link below to set a new password. This link expires in 60 minutes.

            %s

            If you did not request this, you can safely ignore this email.
            Your password will not be changed.

            — NotePlus
            """.formatted(resetLink));
        message.setFrom("noreply@noteplus.nl");

        mailSender.send(message);
    }
}
```

## Step 8 — PasswordResetService

`src/main/java/org/noteplus/noteplus/service/PasswordResetService.java`:

```java
package org.noteplus.noteplus.service;

public interface PasswordResetService {
    void requestReset(String email);
    void resetPassword(String rawToken, String newPassword, String confirmPassword);
}
```

`src/main/java/org/noteplus/noteplus/service/impl/PasswordResetServiceImpl.java`:

- `@Service @RequiredArgsConstructor`
- Inject: `UserRepository`, `PasswordResetTokenRepository`, `EmailService`,
          `TokenUtil`, `PasswordEncoder`
- Read `@Value("${app.base-url}")` and `@Value("${app.password-reset.expiry-minutes}")`

**requestReset():**
```java
@Override
public void requestReset(String email) {
    // Clean up expired/used tokens first
    tokenRepository.deleteExpiredAndUsed(LocalDateTime.now());

    // Find user — but do NOT throw or behave differently if not found
    var userOpt = userRepository.findByEmail(email);

    // SECURITY: always return without revealing whether email exists
    if (userOpt.isEmpty()) {
        return;  // silently do nothing
    }

    var user = userOpt.get();
    String rawToken = tokenUtil.generateRawToken();
    String tokenHash = tokenUtil.hashToken(rawToken);

    var resetToken = new PasswordResetToken();
    resetToken.setTokenHash(tokenHash);
    resetToken.setUser(user);
    resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
    resetToken.setUsed(false);
    tokenRepository.save(resetToken);

    String resetLink = baseUrl + "/reset-password?token=" + rawToken;
    emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
}
```

**resetPassword():**
```java
@Override
public void resetPassword(String rawToken, String newPassword, String confirmPassword) {
    if (!newPassword.equals(confirmPassword)) {
        throw new IllegalArgumentException("Passwords do not match");
    }
    if (newPassword.length() < 8) {
        throw new IllegalArgumentException("Password must be at least 8 characters");
    }

    String tokenHash = tokenUtil.hashToken(rawToken);

    var resetToken = tokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset link"));

    if (resetToken.isUsed()) {
        throw new IllegalArgumentException("This reset link has already been used");
    }

    if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
        throw new IllegalArgumentException("This reset link has expired. Please request a new one");
    }

    var user = resetToken.getUser();
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    resetToken.setUsed(true);
    tokenRepository.save(resetToken);
}
```

Check if `UserRepository` has `findByEmail(String email)`.
If not, add it: `Optional<User> findByEmail(String email);`

## Step 9 — Add endpoints to SecurityConfig

The forgot/reset pages are PUBLIC — users are not logged in when they use them.
Add to permitAll() in SecurityConfig:

```java
"/forgot-password",
"/forgot-password/**",
"/reset-password",
"/reset-password/**",
"/api/auth/forgot-password",
"/api/auth/reset-password"
```

## Step 10 — REST endpoints (add to AuthController)

Read AuthController.java first. Add these two methods:

```java
@PostMapping("/forgot-password")
@Operation(summary = "Request a password reset email")
@ApiResponse(responseCode = "200", description = "If the email is registered, a reset link has been sent")
public ResponseEntity<String> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
    passwordResetService.requestReset(request.email());
    // SECURITY: always return same message regardless of whether email exists
    return ResponseEntity.ok("If this email is registered, you will receive a reset link.");
}

@PostMapping("/reset-password")
@Operation(summary = "Reset password using a token from the reset email")
@ApiResponse(responseCode = "204", description = "Password reset successfully")
@ApiResponse(responseCode = "400", description = "Invalid token, expired, or passwords do not match")
public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
    passwordResetService.resetPassword(request.token(), request.newPassword(), request.confirmPassword());
    return ResponseEntity.noContent().build();
}
```

Create the two request DTOs:

`ForgotPasswordRequest.java`:
```java
public record ForgotPasswordRequest(
    @NotBlank @Email(message = "Please enter a valid email address")
    String email
) {}
```

`ResetPasswordRequest.java`:
```java
public record ResetPasswordRequest(
    @NotBlank(message = "Reset token is required")
    String token,

    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    String newPassword,

    @NotBlank(message = "Please confirm your password")
    String confirmPassword
) {}
```

Add `PasswordResetService` injection to AuthController.

## Step 11 — PasswordResetViewController

`src/main/java/org/noteplus/noteplus/controller/PasswordResetViewController.java`:

```java
@Controller
@RequiredArgsConstructor
public class PasswordResetViewController {

    private final PasswordResetService passwordResetService;

    @GetMapping("/forgot-password")
    public String showForgotPassword(Model model) {
        model.addAttribute("submitted", false);
        model.addAttribute("error", null);
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(
            @RequestParam String email,
            Model model) {
        passwordResetService.requestReset(email);
        // Always show the same message — do not reveal if email exists
        model.addAttribute("submitted", true);
        model.addAttribute("error", null);
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPassword(
            @RequestParam String token,
            Model model) {
        model.addAttribute("token", token);
        model.addAttribute("error", null);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model) {
        try {
            passwordResetService.resetPassword(token, newPassword, confirmPassword);
            return "redirect:/login?passwordReset=true";
        } catch (Exception e) {
            model.addAttribute("token", token);
            model.addAttribute("error", e.getMessage());
            return "auth/reset-password";
        }
    }
}
```

## Step 12 — JTE templates

`src/main/jte/auth/forgot-password.jte`:

```html
@param boolean submitted
@param String error

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Forgot Password — NotePlus</title>
    <style>
        /* reuse same styles as login.jte */
        * { box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
               background: #f5f5f5; margin: 0; min-height: 100vh;
               display: flex; align-items: center; justify-content: center; padding: 16px; }
        .card { background: white; width: 100%; max-width: 400px; padding: 40px 36px;
                border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
        .logo { font-size: 24px; font-weight: 700; color: #1a1a1a; margin-bottom: 6px; }
        .subtitle { font-size: 14px; color: #6b7280; margin-bottom: 28px; }
        label { display: block; font-size: 13px; font-weight: 600; color: #374151;
                margin-top: 16px; margin-bottom: 4px; }
        input { width: 100%; padding: 10px 12px; border: 1px solid #d1d5db;
                border-radius: 6px; font-size: 14px; }
        input:focus { outline: none; border-color: #2563eb; box-shadow: 0 0 0 2px rgba(37,99,235,0.15); }
        .error { background: #fef2f2; color: #dc2626; padding: 10px 14px;
                 border-radius: 6px; font-size: 13px; margin-bottom: 8px; }
        .info { background: #eff6ff; color: #1d4ed8; padding: 12px 14px;
                border-radius: 6px; font-size: 13px; margin-bottom: 8px; }
        button { width: 100%; margin-top: 20px; padding: 11px; background: #2563eb;
                 color: white; border: none; border-radius: 6px; font-size: 15px;
                 font-weight: 600; cursor: pointer; }
        button:hover { background: #1d4ed8; }
        .back { display: block; text-align: center; margin-top: 18px;
                font-size: 13px; color: #2563eb; text-decoration: none; }
    </style>
</head>
<body>
<div class="card">
    <div class="logo">NotePlus</div>
    <div class="subtitle">Reset your password</div>

    @if(error != null)
        <div class="error">${error}</div>
    @endif

    @if(submitted)
        <div class="info">
            If this email is registered, you will receive a password reset link shortly.
            Check your inbox (and spam folder).
        </div>
    @else
        <form method="post" action="/forgot-password">
            <label for="email">Email address</label>
            <input type="email" id="email" name="email" required autocomplete="email"
                   placeholder="you@example.com">
            <button type="submit">Send reset link</button>
        </form>
    @endif

    <a class="back" href="/login">← Back to login</a>
</div>
</body>
</html>
```

`src/main/jte/auth/reset-password.jte`:

```html
@param String token
@param String error

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Reset Password — NotePlus</title>
    <style>
        /* same base styles */
        * { box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
               background: #f5f5f5; margin: 0; min-height: 100vh;
               display: flex; align-items: center; justify-content: center; padding: 16px; }
        .card { background: white; width: 100%; max-width: 400px; padding: 40px 36px;
                border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
        .logo { font-size: 24px; font-weight: 700; color: #1a1a1a; margin-bottom: 6px; }
        .subtitle { font-size: 14px; color: #6b7280; margin-bottom: 28px; }
        label { display: block; font-size: 13px; font-weight: 600; color: #374151;
                margin-top: 16px; margin-bottom: 4px; }
        input { width: 100%; padding: 10px 12px; border: 1px solid #d1d5db;
                border-radius: 6px; font-size: 14px; }
        input:focus { outline: none; border-color: #2563eb; box-shadow: 0 0 0 2px rgba(37,99,235,0.15); }
        .error { background: #fef2f2; color: #dc2626; padding: 10px 14px;
                 border-radius: 6px; font-size: 13px; margin-bottom: 8px; }
        .hint { font-size: 11px; color: #9ca3af; margin-top: 3px; }
        button { width: 100%; margin-top: 24px; padding: 11px; background: #2563eb;
                 color: white; border: none; border-radius: 6px; font-size: 15px;
                 font-weight: 600; cursor: pointer; }
        button:hover { background: #1d4ed8; }
        .back { display: block; text-align: center; margin-top: 18px;
                font-size: 13px; color: #2563eb; text-decoration: none; }
    </style>
</head>
<body>
<div class="card">
    <div class="logo">NotePlus</div>
    <div class="subtitle">Set a new password</div>

    @if(error != null)
        <div class="error">${error}</div>
    @endif

    <form method="post" action="/reset-password">
        <input type="hidden" name="token" value="${token}">

        <label for="newPassword">New password</label>
        <input type="password" id="newPassword" name="newPassword"
               required minlength="8" autocomplete="new-password">
        <p class="hint">At least 8 characters</p>

        <label for="confirmPassword">Confirm new password</label>
        <input type="password" id="confirmPassword" name="confirmPassword"
               required minlength="8" autocomplete="new-password">

        <button type="submit">Set new password</button>
    </form>

    <a class="back" href="/login">← Back to login</a>
</div>
</body>
</html>
```

## Step 13 — Update login.jte to show password reset success

Read `login.jte`. Add at the top of the form section, after the error block:

```html
@param(default = "") String passwordReset

@if(passwordReset.equals("true"))
    <div style="background:#f0fdf4;color:#16a34a;padding:10px 14px;border-radius:6px;font-size:13px;margin-bottom:14px;">
        Password reset successfully. You can now sign in.
    </div>
@endif
```

And update the `@GetMapping("/login")` in AuthViewController to pass this param:
```java
model.addAttribute("passwordReset", request.getParameter("passwordReset"));
```

## Step 14 — Test

| Test | Expected |
|------|----------|
| GET /forgot-password | Form renders |
| POST /forgot-password — unknown email | Same "check your inbox" message |
| POST /forgot-password — known email | Same message + email in Mailtrap |
| Click link in Mailtrap email | Reset form renders with token in URL |
| POST /reset-password — mismatched passwords | "Passwords do not match" |
| POST /reset-password — valid token + valid passwords | Redirect to /login?passwordReset=true |
| POST /reset-password — same token again | "This reset link has already been used" |
| POST /reset-password — expired token | "This reset link has expired" |
| Login with new password | Success |
| Login with old password | "Invalid username or password." |
