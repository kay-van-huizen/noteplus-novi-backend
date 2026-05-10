package org.noteplus.noteplus.controller;

import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.CreateLearningPathRequest;
import org.noteplus.noteplus.repository.UserRepository;
import org.noteplus.noteplus.service.LearningPathService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/learning-paths")
@RequiredArgsConstructor
public class LearningPathViewController {

    private final LearningPathService learningPathService;
    private final UserRepository userRepository;

    @GetMapping
    public String list(Model model, Authentication auth) {
        model.addAttribute("paths", learningPathService.getAllForUser(auth.getName()));
        return "learning-paths/list";
    }

    @GetMapping("/new")
    public String showForm(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("error", null);
        return "learning-paths/form";
    }

    @PostMapping
    public String handleCreate(
            @ModelAttribute CreateLearningPathRequest request,
            Authentication auth,
            Model model) {
        try {
            learningPathService.create(request, auth.getName());
            return "redirect:/learning-paths";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("users", userRepository.findAll());
            return "learning-paths/form";
        }
    }
}
