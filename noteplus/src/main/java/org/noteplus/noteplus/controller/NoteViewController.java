package org.noteplus.noteplus.controller;

import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.CreateNoteRequest;
import org.noteplus.noteplus.dto.request.UpdateNoteRequest;
import org.noteplus.noteplus.dto.response.NoteResponse;
import org.noteplus.noteplus.dto.response.ReferenceResponse;
import org.noteplus.noteplus.service.CategoryService;
import org.noteplus.noteplus.service.NoteService;
import org.noteplus.noteplus.service.ReferenceService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NoteViewController {

    private final NoteService noteService;
    private final CategoryService categoryService;
    private final ReferenceService referenceService;

    @GetMapping
    public String list(Model model, Authentication auth) {
        model.addAttribute("notes", noteService.getAllForUser(auth.getName()));
        return "notes/list";
    }

    @GetMapping("/new")
    public String showForm(Model model) {
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("error", null);
        return "notes/form";
    }

    @PostMapping
    public String handleCreate(
            @ModelAttribute CreateNoteRequest request,
            Authentication auth,
            Model model) {
        try {
            noteService.create(request, auth.getName());
            return "redirect:/notes";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.getAll());
            return "notes/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEdit(@PathVariable Long id, Model model, Authentication auth) {
        try {
            NoteResponse note = noteService.getById(id, auth.getName());
            List<ReferenceResponse> references = referenceService.getAllForNote(id, auth.getName());
            model.addAttribute("note", note);
            model.addAttribute("references", references);
            model.addAttribute("error", null);
            return "notes/edit";
        } catch (Exception e) {
            return "redirect:/notes";
        }
    }

    @PostMapping("/{id}/edit")
    public String handleUpdate(
            @PathVariable Long id,
            @ModelAttribute UpdateNoteRequest request,
            Authentication auth,
            Model model) {
        try {
            noteService.update(id, request, auth.getName());
            return "redirect:/notes/" + id + "/edit?saved=true";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            try {
                model.addAttribute("note", noteService.getById(id, auth.getName()));
                model.addAttribute("references", referenceService.getAllForNote(id, auth.getName()));
            } catch (Exception ignored) {}
            return "notes/edit";
        }
    }
}
