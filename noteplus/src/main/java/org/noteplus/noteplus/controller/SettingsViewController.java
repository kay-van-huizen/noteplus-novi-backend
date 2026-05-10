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
