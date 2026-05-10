---
description: Builds the Note JTE frontend — NoteViewController, form template (create), and list template (own notes overview). Run this only after note-backend.md is complete and all Swagger tests pass.
allowed-tools: Read, Write, Bash(./mvnw compile:*), Glob, Grep
---

# Note Frontend — NotePlus (JTE)

## Step 0 — Read context first

Before writing any code, read:

```
src/main/java/org/noteplus/noteplus/entity/Note.java
src/main/java/org/noteplus/noteplus/service/NoteService.java
src/main/java/org/noteplus/noteplus/service/CategoryService.java
src/main/java/org/noteplus/noteplus/dto/response/NoteResponse.java
src/main/java/org/noteplus/noteplus/dto/response/CategoryResponse.java
src/main/jte/categories/form.jte                   ← use as style reference
src/main/jte/categories/list.jte                   ← use as style reference
src/main/resources/application.properties          ← confirm gg.jte.developmentMode=true
```

Verify the exact getter method names on NoteResponse and CategoryResponse before writing templates.
For example: is it categoryTitle() or categoryName()? Check the record definition.

## Step 1 — NoteViewController

This is a separate controller from the REST NoteController.
It serves HTML pages via JTE templates, not JSON.

`src/main/java/org/noteplus/noteplus/controller/NoteViewController.java`:

```java
package org.noteplus.noteplus.controller;

import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.CreateNoteRequest;
import org.noteplus.noteplus.service.CategoryService;
import org.noteplus.noteplus.service.NoteService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NoteViewController {

    private final NoteService noteService;
    private final CategoryService categoryService;

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
}
```

Add `/notes/**` to the permitted paths in SecurityConfig, same pattern as `/categories/**`:
```java
.requestMatchers(
    "/api/auth/**",
    "/categories/**",
    "/notes/**",        // ← add this
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/error"
).permitAll()
```

## Step 2 — Create the template directory

Create `src/main/jte/notes/` if it does not exist.

## Step 3 — Form template

`src/main/jte/notes/form.jte`:

Read CategoryResponse.java first to confirm the exact getter names (id(), title(), etc.)
before writing the @for loop options.

```html
@import org.noteplus.noteplus.dto.response.CategoryResponse
@import java.util.List

@param List<CategoryResponse> categories
@param String error

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create Note — NotePlus</title>
    <style>
        * { box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            background: #f5f5f5;
            margin: 0;
            padding: 40px 16px;
        }
        .card {
            background: white;
            max-width: 600px;
            margin: 0 auto;
            padding: 32px;
            border-radius: 8px;
            box-shadow: 0 1px 4px rgba(0,0,0,0.1);
        }
        h1 { font-size: 22px; margin: 0 0 24px; color: #1a1a1a; }
        label {
            display: block;
            font-size: 14px;
            font-weight: 600;
            color: #333;
            margin-top: 16px;
            margin-bottom: 4px;
        }
        input, select, textarea {
            width: 100%;
            padding: 8px 12px;
            border: 1px solid #d1d5db;
            border-radius: 6px;
            font-size: 14px;
            color: #1a1a1a;
            font-family: inherit;
        }
        input:focus, select:focus, textarea:focus {
            outline: none;
            border-color: #2563eb;
            box-shadow: 0 0 0 2px rgba(37,99,235,0.15);
        }
        textarea { min-height: 180px; resize: vertical; }
        .error {
            background: #fef2f2;
            color: #dc2626;
            padding: 10px 14px;
            border-radius: 6px;
            font-size: 14px;
            margin-bottom: 16px;
        }
        .hint { font-size: 12px; color: #9ca3af; margin-top: 4px; }
        .actions { display: flex; gap: 12px; margin-top: 28px; }
        button[type=submit] {
            padding: 10px 24px;
            background: #2563eb;
            color: white;
            border: none;
            border-radius: 6px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
        }
        button[type=submit]:hover { background: #1d4ed8; }
        a.cancel {
            padding: 10px 24px;
            color: #6b7280;
            text-decoration: none;
            font-size: 14px;
            line-height: 1.6;
        }
    </style>
</head>
<body>
<div class="card">
    <h1>Create Note</h1>

    @if(error != null)
        <div class="error">${error}</div>
    @endif

    <form method="post" action="/notes">
        <label for="title">Title *</label>
        <input type="text" id="title" name="title" required maxlength="255" placeholder="e.g. Spring Boot annotations">

        <label for="content">Content *</label>
        <textarea id="content" name="content" required placeholder="Write your note here..."></textarea>

        <label for="categoryId">Category (optional)</label>
        <select id="categoryId" name="categoryId">
            <option value="">— no category —</option>
            @for(CategoryResponse cat : categories)
                <option value="${cat.id()}">${cat.title()}</option>
            @endfor
        </select>
        <p class="hint">Link this note to a category to keep things organised.</p>

        <div class="actions">
            <button type="submit">Save Note</button>
            <a class="cancel" href="/notes">Cancel</a>
        </div>
    </form>
</div>
</body>
</html>
```

If CategoryResponse uses getName() instead of getTitle(), change cat.title() to cat.name() above.

## Step 4 — List template

`src/main/jte/notes/list.jte`:

```html
@import org.noteplus.noteplus.dto.response.NoteResponse
@import java.util.List

@param List<NoteResponse> notes

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>My Notes — NotePlus</title>
    <style>
        * { box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            background: #f5f5f5;
            margin: 0;
            padding: 40px 16px;
        }
        .card {
            background: white;
            max-width: 760px;
            margin: 0 auto;
            border-radius: 8px;
            box-shadow: 0 1px 4px rgba(0,0,0,0.1);
            overflow: hidden;
        }
        .card-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 24px 28px;
            border-bottom: 1px solid #e5e7eb;
        }
        h1 { font-size: 20px; margin: 0; color: #1a1a1a; }
        a.btn {
            padding: 8px 18px;
            background: #2563eb;
            color: white;
            text-decoration: none;
            border-radius: 6px;
            font-size: 14px;
            font-weight: 600;
        }
        table { width: 100%; border-collapse: collapse; }
        th {
            text-align: left;
            padding: 12px 28px;
            font-size: 12px;
            font-weight: 600;
            color: #6b7280;
            text-transform: uppercase;
            letter-spacing: .05em;
            border-bottom: 1px solid #e5e7eb;
        }
        td {
            padding: 14px 28px;
            font-size: 14px;
            color: #1a1a1a;
            border-bottom: 1px solid #f3f4f6;
            vertical-align: top;
        }
        tr:last-child td { border-bottom: none; }
        .preview {
            color: #6b7280;
            font-size: 13px;
            margin-top: 2px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            max-width: 360px;
        }
        .badge {
            display: inline-block;
            padding: 2px 10px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 500;
            background: #e0e7ff;
            color: #3730a3;
        }
        .date { color: #9ca3af; font-size: 12px; }
        .empty {
            padding: 48px 28px;
            text-align: center;
            color: #9ca3af;
            font-size: 14px;
        }
    </style>
</head>
<body>
<div class="card">
    <div class="card-header">
        <h1>My Notes</h1>
        <a class="btn" href="/notes/new">+ New Note</a>
    </div>

    @if(notes.isEmpty())
        <div class="empty">You have no notes yet. Create your first note!</div>
    @else
        <table>
            <thead>
                <tr>
                    <th>Title</th>
                    <th>Category</th>
                    <th>Created</th>
                </tr>
            </thead>
            <tbody>
                @for(NoteResponse note : notes)
                    <tr>
                        <td>
                            <strong>${note.title()}</strong>
                            <div class="preview">${note.content()}</div>
                        </td>
                        <td>
                            @if(note.categoryTitle() != null)
                                <span class="badge">${note.categoryTitle()}</span>
                            @else
                                —
                            @endif
                        </td>
                        <td class="date">
                            ${note.createdAt() != null ? note.createdAt().toLocalDate().toString() : "—"}
                        </td>
                    </tr>
                @endfor
            </tbody>
        </table>
    @endif
</div>
</body>
</html>
```

## Step 5 — Compile and browser test

```bash
./mvnw compile
./mvnw spring-boot:run
```

Browser tests:
- http://localhost:8080/notes → list renders, shows empty state for a new user
- http://localhost:8080/notes/new → form renders with category dropdown populated
- Submit with title + content → redirects to /notes, note visible in list
- Submit with empty title → form re-renders with validation error message
- Submit with a category selected → note shows category badge in list

If JTE throws a rendering error:
- Check that all getter names in templates match the actual record field names
- Confirm gg.jte.developmentMode=true is set in application.properties
- Verify all @import package paths are correct
