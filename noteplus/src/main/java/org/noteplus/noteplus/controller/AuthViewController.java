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
    public String showLogin(
            @RequestParam(defaultValue = "") String passwordReset,
            Model model) {
        model.addAttribute("error", null);
        model.addAttribute("passwordReset", passwordReset);
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
            return "redirect:/notes";
        } catch (Exception e) {
            // SECURITY: never reveal whether the username or password was wrong
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
            return "redirect:/notes";
        } catch (Exception e) {
            // SECURITY: don't confirm whether a specific username or email already exists
            model.addAttribute("error", "Username or email is already in use. Please try different credentials.");
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
