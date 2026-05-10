---
description: Builds the LearningPath JTE frontend — LearningPathViewController, form template with student/coach dropdowns, and list template showing assigned paths. Run only after learning-path-backend.md is complete and all Swagger tests pass.
allowed-tools: Read, Write, Bash(./mvnw compile:*), Glob, Grep
---

# LearningPath Frontend — NotePlus (JTE)

## Step 0 — Read context first

Before writing any code, read:

```
src/main/java/org/noteplus/noteplus/entity/LearningPath.java
src/main/java/org/noteplus/noteplus/service/LearningPathService.java
src/main/java/org/noteplus/noteplus/dto/response/LearningPathResponse.java
src/main/java/org/noteplus/noteplus/dto/response/NoteResponse.java
src/main/java/org/noteplus/noteplus/repository/UserRepository.java
src/main/jte/notes/form.jte                    ← use as style reference
src/main/jte/notes/list.jte                    ← use as style reference
src/main/resources/application.properties      ← confirm gg.jte.developmentMode=true
```

The form needs a dropdown of all users to select the student and the coach.
We need to fetch all users from the UserRepository and pass them to the template.
The ViewController is responsible for loading these lists — not the template itself.

## Step 1 — LearningPathViewController

`src/main/java/org/noteplus/noteplus/controller/LearningPathViewController.java`:

```java
package org.noteplus.noteplus.controller;

import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.CreateLearningPathRequest;
import org.noteplus.noteplus.dto.response.UserResponse;
import org.noteplus.noteplus.repository.UserRepository;
import org.noteplus.noteplus.service.LearningPathService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        // Load all users for the student and coach dropdowns
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
```

If a `UserResponse` DTO does not exist yet, create a simple one:
`src/main/java/org/noteplus/noteplus/dto/response/UserResponse.java`:
```java
public record UserResponse(Long id, String username, String email) {}
```

If the UserRepository does not have `findAll()` returning User entities directly,
use `userRepository.findAll()` — JpaRepository provides this by default.

Add `/learning-paths/**` to permitted paths in SecurityConfig:
```java
.requestMatchers(
    "/api/auth/**",
    "/categories/**",
    "/notes/**",
    "/learning-paths/**",    // ← add this
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/error"
).permitAll()
```

## Step 2 — Create template directory

Create `src/main/jte/learning-paths/` if it does not exist.

## Step 3 — Form template

`src/main/jte/learning-paths/form.jte`:

Read the User entity fields first to confirm available getters (getId(), getUsername(), etc.)

```html
@import org.noteplus.noteplus.entity.User
@import java.util.List

@param List<User> users
@param String error

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create Learning Path — NotePlus</title>
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
            max-width: 560px;
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
        textarea { min-height: 100px; resize: vertical; }
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
    <h1>Create Learning Path</h1>

    @if(error != null)
        <div class="error">${error}</div>
    @endif

    <form method="post" action="/learning-paths">
        <label for="title">Title *</label>
        <input type="text" id="title" name="title" required maxlength="255"
               placeholder="e.g. Java Backend Development">

        <label for="description">Description</label>
        <textarea id="description" name="description"
                  placeholder="Describe the goal of this learning path..."></textarea>

        <label for="studentId">Assign to Student *</label>
        <select id="studentId" name="studentId" required>
            <option value="">— select a student —</option>
            @for(User user : users)
                <option value="${user.getId()}">${user.getUsername()} (${user.getEmail()})</option>
            @endfor
        </select>
        <p class="hint">The student this learning path is designed for.</p>

        <label for="coachId">Assign Coach *</label>
        <select id="coachId" name="coachId" required>
            <option value="">— select a coach —</option>
            @for(User user : users)
                <option value="${user.getId()}">${user.getUsername()} (${user.getEmail()})</option>
            @endfor
        </select>
        <p class="hint">The coach responsible for guiding this learning path.</p>

        <div class="actions">
            <button type="submit">Create Path</button>
            <a class="cancel" href="/learning-paths">Cancel</a>
        </div>
    </form>
</div>
</body>
</html>
```

Note: both dropdowns show all users. In a real production app you would filter by role.
For this school project showing all users is acceptable and keeps the implementation simple.

## Step 4 — List template

`src/main/jte/learning-paths/list.jte`:

```html
@import org.noteplus.noteplus.dto.response.LearningPathResponse
@import org.noteplus.noteplus.dto.response.NoteResponse
@import java.util.List

@param List<LearningPathResponse> paths

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Learning Paths — NotePlus</title>
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
            max-width: 800px;
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
        .path-row {
            padding: 20px 28px;
            border-bottom: 1px solid #f3f4f6;
        }
        .path-row:last-child { border-bottom: none; }
        .path-title { font-size: 16px; font-weight: 600; color: #1a1a1a; margin-bottom: 4px; }
        .path-desc { font-size: 13px; color: #6b7280; margin-bottom: 10px; }
        .meta { display: flex; gap: 24px; font-size: 12px; color: #6b7280; margin-bottom: 10px; }
        .meta span strong { color: #374151; }
        .notes-label { font-size: 12px; font-weight: 600; color: #6b7280;
                       text-transform: uppercase; letter-spacing: .05em; margin-bottom: 6px; }
        .note-chips { display: flex; flex-wrap: wrap; gap: 6px; }
        .chip {
            display: inline-block;
            padding: 3px 10px;
            background: #e0e7ff;
            color: #3730a3;
            border-radius: 12px;
            font-size: 12px;
        }
        .no-notes { font-size: 12px; color: #d1d5db; font-style: italic; }
        .empty {
            padding: 48px 28px;
            text-align: center;
            color: #9ca3af;
            font-size: 14px;
        }
        .date { font-size: 11px; color: #d1d5db; margin-top: 8px; }
    </style>
</head>
<body>
<div class="card">
    <div class="card-header">
        <h1>Learning Paths</h1>
        <a class="btn" href="/learning-paths/new">+ New Path</a>
    </div>

    @if(paths.isEmpty())
        <div class="empty">No learning paths yet. Create your first one!</div>
    @else
        @for(LearningPathResponse path : paths)
            <div class="path-row">
                <div class="path-title">${path.title()}</div>

                @if(path.description() != null && !path.description().isEmpty())
                    <div class="path-desc">${path.description()}</div>
                @endif

                <div class="meta">
                    <span>Student: <strong>${path.studentUsername()}</strong></span>
                    <span>Coach: <strong>${path.coachUsername()}</strong></span>
                </div>

                <div class="notes-label">Notes (${path.notes().size()})</div>
                @if(path.notes().isEmpty())
                    <div class="no-notes">No notes linked yet.</div>
                @else
                    <div class="note-chips">
                        @for(NoteResponse note : path.notes())
                            <span class="chip">${note.title()}</span>
                        @endfor
                    </div>
                @endif

                <div class="date">Created: ${path.createdAt() != null ? path.createdAt().toLocalDate().toString() : "—"}</div>
            </div>
        @endfor
    @endif
</div>
</body>
</html>
```

## Step 5 — Browser test

Browser tests:
- http://localhost:8080/learning-paths → list renders, shows empty state
- http://localhost:8080/learning-paths/new → form renders with two user dropdowns populated
- Select a student, select a coach, fill in a title → submit → redirect to list, path visible
- Submit without coachId → form re-renders with validation error
- Path in list shows student username, coach username, and note count (0)

If JTE throws a rendering error:
- Verify all getter method names against the actual entity and DTO definitions
- Check User entity getters: getId(), getUsername(), getEmail()
- Check LearningPathResponse getters: studentUsername(), coachUsername(), notes()
- Confirm gg.jte.developmentMode=true in application.properties
