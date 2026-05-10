---
description: Builds the login and registration JTE frontend — AuthViewController, login.jte, register.jte, HTTP-only JWT cookie handling, and updates JwtAuthenticationFilter to read cookies. Run after openapi-config is complete.
allowed-tools: Read, Write, Bash(git checkout:*), Bash(git pull:*), Bash(./mvnw compile:*), Glob, Grep
---

# Login & Registration Frontend — NotePlus (JTE)

## Architectural decision: HTTP-only cookie for JWT

The backend is STATELESS — no server sessions.
JTE is server-side rendered — browsers send form POSTs, not Authorization headers.

Solution: after login, the server sets the JWT as an HTTP-only cookie.
The browser sends this cookie automatically with every subsequent request.
The JwtAuthenticationFilter must be updated to read from both the cookie AND the
Authorization header (so Swagger and Postman continue to work).

HTTP-only means JavaScript cannot read the cookie — this prevents XSS token theft.

## Security rules for this feature

- Cookie must be HTTP-only and SameSite=Strict
- Cookie must have a max-age matching the JWT expiry (86400 seconds = 24 hours)
- Logout must explicitly clear the cookie
- Login errors must NEVER reveal whether the username or password was wrong
  → Always return: "Invalid username or password" — not "User not found" or "Wrong password"
- Registration errors must NOT reveal that a specific email/username already exists
  in the generic error message on the form (though a 409 is fine for the API)

## Step 0 — Read context first (MANDATORY)

```
src/main/java/org/noteplus/noteplus/security/JwtAuthenticationFilter.java  ← modify this
src/main/java/org/noteplus/noteplus/security/JwtService.java
src/main/java/org/noteplus/noteplus/security/SecurityConfig.java
src/main/java/org/noteplus/noteplus/service/AuthService.java
src/main/java/org/noteplus/noteplus/service/impl/AuthServiceImpl.java
src/main/java/org/noteplus/noteplus/dto/request/LoginRequest.java
src/main/java/org/noteplus/noteplus/dto/request/RegisterRequest.java
src/main/java/org/noteplus/noteplus/dto/response/AuthResponse.java
src/main/java/org/noteplus/noteplus/security/JwtProperties.java
src/main/jte/categories/form.jte                                             ← style reference
src/main/resources/application.properties
CLAUDE.md
```

## Step 2 — Update JwtAuthenticationFilter to read cookies

Read the existing `JwtAuthenticationFilter.java` carefully first.
The filter currently reads the token from the `Authorization: Bearer` header only.

Update `doFilterInternal` to also check for a cookie named `jwt` if no header is present:

```java
private String extractToken(HttpServletRequest request) {
    // 1. Try Authorization header first (for Swagger, Postman, API clients)
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        return authHeader.substring(7);
    }

    // 2. Fallback: try HTTP-only cookie (for browser / JTE form requests)
    if (request.getCookies() != null) {
        for (Cookie cookie : request.getCookies()) {
            if ("jwt".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
    }

    return null;  // no token found
}
```

Replace the inline header reading logic in `doFilterInternal` with a call to this helper:
```java
String token = extractToken(request);
if (token == null) {
    filterChain.doFilter(request, response);
    return;
}
```

The rest of the filter (validate → set SecurityContext) remains unchanged.

## Step 3 — Add cookie helper to SecurityConfig or a CookieUtil class

Create `src/main/java/org/noteplus/noteplus/util/CookieUtil.java`:

```java
package org.noteplus.noteplus.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    public void addJwtCookie(HttpServletResponse response, String token, long expirySeconds) {
        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);          // JS cannot read this cookie
        cookie.setSecure(false);           // set to true in production (HTTPS only)
        cookie.setPath("/");               // available for all paths
        cookie.setMaxAge((int) expirySeconds);
        // SameSite=Strict via header — Cookie API does not support this directly
        response.addCookie(cookie);
        response.addHeader("Set-Cookie",
            "jwt=" + token + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + expirySeconds);
    }

    public void clearJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);              // immediate expiry
        response.addCookie(cookie);
    }
}
```

## Step 4 — Add auth view routes to SecurityConfig

Read SecurityConfig.java and add the following paths to permitAll():

```java
"/login",
"/login/**",
"/register",
"/register/**",
"/logout-page"
```

## Step 5 — AuthViewController

`src/main/java/org/noteplus/noteplus/controller/AuthViewController.java`:

```java
package org.noteplus.noteplus.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.LoginRequest;
import org.noteplus.noteplus.dto.request.RegisterRequest;
import org.noteplus.noteplus.dto.response.AuthResponse;
import org.noteplus.noteplus.security.JwtProperties;
import org.noteplus.noteplus.service.AuthService;
import org.noteplus.noteplus.util.CookieUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AuthViewController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final JwtProperties jwtProperties;

    // --- Login ---

    @GetMapping("/login")
    public String showLogin(Model model) {
        model.addAttribute("error", null);
        return "auth/login";
    }

    @PostMapping("/login")
    public String handleLogin(
            @Valid @ModelAttribute LoginRequest request,
            BindingResult bindingResult,
            HttpServletResponse response,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Please fill in all fields.");
            return "auth/login";
        }

        try {
            AuthResponse auth = authService.login(request);
            long expirySeconds = jwtProperties.getExpirationMs() / 1000;
            cookieUtil.addJwtCookie(response, auth.token(), expirySeconds);
            return "redirect:/notes";  // redirect to main page after login
        } catch (Exception e) {
            // SECURITY: never reveal whether username or password was wrong
            model.addAttribute("error", "Invalid username or password.");
            return "auth/login";
        }
    }

    // --- Register ---

    @GetMapping("/register")
    public String showRegister(Model model) {
        model.addAttribute("error", null);
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(
            @Valid @ModelAttribute RegisterRequest request,
            BindingResult bindingResult,
            HttpServletResponse response,
            Model model) {

        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .findFirst()
                    .orElse("Invalid input.");
            model.addAttribute("error", message);
            return "auth/register";
        }

        try {
            AuthResponse auth = authService.register(request);
            long expirySeconds = jwtProperties.getExpirationMs() / 1000;
            cookieUtil.addJwtCookie(response, auth.token(), expirySeconds);
            return "redirect:/notes";  // auto-login after register
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    // --- Logout ---

    @GetMapping("/logout-page")
    public String logout(HttpServletResponse response) {
        cookieUtil.clearJwtCookie(response);
        return "redirect:/login";
    }
}
```

## Step 6 — login.jte

Create directory `src/main/jte/auth/`.

`src/main/jte/auth/login.jte`:

```html
@param String error

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login — NotePlus</title>
    <style>
        * { box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            background: #f5f5f5;
            margin: 0;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 16px;
        }
        .card {
            background: white;
            width: 100%;
            max-width: 400px;
            padding: 40px 36px;
            border-radius: 10px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        .logo { font-size: 24px; font-weight: 700; color: #1a1a1a; margin-bottom: 6px; }
        .subtitle { font-size: 14px; color: #6b7280; margin-bottom: 28px; }
        label {
            display: block;
            font-size: 13px;
            font-weight: 600;
            color: #374151;
            margin-top: 16px;
            margin-bottom: 4px;
        }
        input {
            width: 100%;
            padding: 10px 12px;
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
            margin-bottom: 8px;
        }
        button {
            width: 100%;
            margin-top: 24px;
            padding: 11px;
            background: #2563eb;
            color: white;
            border: none;
            border-radius: 6px;
            font-size: 15px;
            font-weight: 600;
            cursor: pointer;
        }
        button:hover { background: #1d4ed8; }
        .footer { margin-top: 20px; text-align: center; font-size: 13px; color: #6b7280; }
        .footer a { color: #2563eb; text-decoration: none; }
        .forgot { display: block; text-align: right; font-size: 12px; color: #2563eb;
                  text-decoration: none; margin-top: 6px; }
    </style>
</head>
<body>
<div class="card">
    <div class="logo">NotePlus</div>
    <div class="subtitle">Sign in to your account</div>

    @if(error != null)
        <div class="error">${error}</div>
    @endif

    <form method="post" action="/login">
        <label for="username">Username</label>
        <input type="text" id="username" name="username" required autocomplete="username">

        <label for="password">Password</label>
        <input type="password" id="password" name="password" required autocomplete="current-password">
        <a class="forgot" href="/forgot-password">Forgot password?</a>

        <button type="submit">Sign in</button>
    </form>

    <div class="footer">
        Don't have an account? <a href="/register">Register</a>
    </div>
</div>
</body>
</html>
```

## Step 7 — register.jte

`src/main/jte/auth/register.jte`:

```html
@param String error

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Register — NotePlus</title>
    <style>
        * { box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            background: #f5f5f5;
            margin: 0;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 16px;
        }
        .card {
            background: white;
            width: 100%;
            max-width: 420px;
            padding: 40px 36px;
            border-radius: 10px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        .logo { font-size: 24px; font-weight: 700; color: #1a1a1a; margin-bottom: 6px; }
        .subtitle { font-size: 14px; color: #6b7280; margin-bottom: 28px; }
        label {
            display: block;
            font-size: 13px;
            font-weight: 600;
            color: #374151;
            margin-top: 16px;
            margin-bottom: 4px;
        }
        input {
            width: 100%;
            padding: 10px 12px;
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
            margin-bottom: 8px;
        }
        .hint { font-size: 11px; color: #9ca3af; margin-top: 3px; }
        button {
            width: 100%;
            margin-top: 24px;
            padding: 11px;
            background: #2563eb;
            color: white;
            border: none;
            border-radius: 6px;
            font-size: 15px;
            font-weight: 600;
            cursor: pointer;
        }
        button:hover { background: #1d4ed8; }
        .footer { margin-top: 20px; text-align: center; font-size: 13px; color: #6b7280; }
        .footer a { color: #2563eb; text-decoration: none; }
    </style>
</head>
<body>
<div class="card">
    <div class="logo">NotePlus</div>
    <div class="subtitle">Create your account</div>

    @if(error != null)
        <div class="error">${error}</div>
    @endif

    <form method="post" action="/register">
        <label for="username">Username</label>
        <input type="text" id="username" name="username" required
               minlength="3" maxlength="50" autocomplete="username">
        <p class="hint">3–50 characters</p>

        <label for="email">Email address</label>
        <input type="email" id="email" name="email" required autocomplete="email">

        <label for="password">Password</label>
        <input type="password" id="password" name="password" required
               minlength="8" autocomplete="new-password">
        <p class="hint">At least 8 characters</p>

        <button type="submit">Create account</button>
    </form>

    <div class="footer">
        Already have an account? <a href="/login">Sign in</a>
    </div>
</div>
</body>
</html>
```

## Step 8 — Test

Browser tests:

| Test | Expected |
|------|----------|
| http://localhost:8080/login | Login form renders |
| POST login with valid credentials | Redirect to /notes, jwt cookie set |
| POST login with wrong password | "Invalid username or password." — no specifics |
| POST login with empty fields | "Please fill in all fields." |
| http://localhost:8080/register | Register form renders |
| POST register with valid data | Redirect to /notes, auto-logged in |
| POST register with existing username | Error message shown |
| http://localhost:8080/logout-page | Cookie cleared, redirect to /login |
| Access /notes without cookie | Redirect to /login (or 401) |

Open browser DevTools → Application → Cookies:
- After login: `jwt` cookie must be present, HttpOnly must be checked, SameSite=Strict
- After logout: `jwt` cookie must be gone or have MaxAge=0

Swagger must still work: http://localhost:8080/swagger-ui/index.html
→ Authorize with Bearer token → GET /api/notes → 200 OK
(Swagger uses Authorization header, not cookie — both paths must work)
