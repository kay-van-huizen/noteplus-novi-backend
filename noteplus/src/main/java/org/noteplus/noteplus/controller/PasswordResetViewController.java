package org.noteplus.noteplus.controller;

import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.service.PasswordResetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
        // SECURITY: always show the same message — do not reveal if email exists
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
