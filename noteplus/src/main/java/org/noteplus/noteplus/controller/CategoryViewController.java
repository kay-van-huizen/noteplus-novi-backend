package org.noteplus.noteplus.controller;

import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.CreateCategoryRequest;
import org.noteplus.noteplus.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryViewController {

    private final CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.getAll());
        return "categories/list";
    }

    @GetMapping("/new")
    public String showForm(Model model) {
        model.addAttribute("parentOptions", categoryService.getAll());
        model.addAttribute("error", null);
        return "categories/form";
    }

    @PostMapping
    public String handleCreate(@ModelAttribute CreateCategoryRequest request, Model model) {
        try {
            categoryService.create(request);
            return "redirect:/categories";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("parentOptions", categoryService.getAll());
            return "categories/form";
        }
    }
}
