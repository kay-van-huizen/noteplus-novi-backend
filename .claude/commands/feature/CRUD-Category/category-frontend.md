---
description: Builds the Category JTE frontend — ViewController, form template (create), and list template (overview). Run this only after category-backend.md is complete and compiling.
allowed-tools: Read, Write, Bash(./mvnw compile:*), Glob, Grep
---

# Category Frontend — NotePlus (JTE)

## Step 0 — Read context first

Before writing any code, read:
- `src/main/java/org/noteplus/noteplus/entity/Category.java` — CategoryColor and CategoryStatus enum values
- `src/main/java/org/noteplus/noteplus/service/CategoryService.java` — available methods
- `src/main/java/org/noteplus/noteplus/dto/response/CategoryResponse.java` — fields available in templates
- `src/main/resources/application.properties` — confirm gg.jte.developmentMode=true is set
- Any existing .jte files in `src/main/jte/` — use as style/pattern reference

## Step 1 — CategoryViewController

This is a separate controller from the REST CategoryController.
It serves HTML pages, not JSON.

`src/main/java/org/noteplus/noteplus/controller/CategoryViewController.java`:

```java
package org.noteplus.noteplus.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.CreateCategoryRequest;
import org.noteplus.noteplus.service.CategoryService;
import org.springframework.security.core.Authentication;
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
    public String handleCreate(
            @Valid @ModelAttribute CreateCategoryRequest request,
            Authentication auth,
            Model model) {
        try {
            categoryService.create(request, auth.getName());
            return "redirect:/categories";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("parentOptions", categoryService.getAll());
            return "categories/form";
        }
    }
}
```

Then add `/categories/**` to the permitted paths in SecurityConfig:
```java
.requestMatchers(
    "/api/auth/**",
    "/categories/**",      // ← add this
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/error"
).permitAll()
```

Note: for a real app you would protect these with authentication.
For this school project, keeping it open is fine to test the forms easily.

## Step 2 — Directory structure

Create the directory `src/main/jte/categories/` if it does not exist.

## Step 3 — Form template (aanmaken)

`src/main/jte/categories/form.jte`:

Read the CategoryColor and CategoryStatus enum values from the entity first,
then generate the <option> elements for each enum value.

```html
@import org.noteplus.noteplus.entity.CategoryColor
@import org.noteplus.noteplus.entity.CategoryStatus
@import org.noteplus.noteplus.dto.response.CategoryResponse
@import java.util.List

@param List<CategoryResponse> parentOptions
@param String error

<!DOCTYPE html>
<html lang="nl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Categorie aanmaken — NotePlus</title>
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
            max-width: 480px;
            margin: 0 auto;
            padding: 32px;
            border-radius: 8px;
            box-shadow: 0 1px 4px rgba(0,0,0,0.1);
        }
        h1 { font-size: 22px; margin: 0 0 24px; color: #1a1a1a; }
        label { display: block; font-size: 14px; font-weight: 600; color: #333; margin-top: 16px; margin-bottom: 4px; }
        input, select {
            width: 100%;
            padding: 8px 12px;
            border: 1px solid #d1d5db;
            border-radius: 6px;
            font-size: 14px;
            color: #1a1a1a;
        }
        input:focus, select:focus {
            outline: none;
            border-color: #2563eb;
            box-shadow: 0 0 0 2px rgba(37,99,235,0.15);
        }
        .error {
            background: #fef2f2;
            color: #dc2626;
            padding: 10px 14px;
            border-radius: 6px;
            font-size: 14px;
            margin-bottom: 16px;
        }
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
    <h1>Categorie aanmaken</h1>

    @if(error != null)
        <div class="error">${error}</div>
    @endif

    <form method="post" action="/categories">
        <label for="name">Naam *</label>
        <input type="text" id="name" name="name" required maxlength="100" placeholder="Bijv. Wiskunde">

        <label for="color">Kleur</label>
        <select id="color" name="color">
            @for(CategoryColor c : CategoryColor.values())
                <option value="${c.name()}">${c.name()}</option>
            @endfor
        </select>

        <label for="status">Status</label>
        <select id="status" name="status">
            @for(CategoryStatus s : CategoryStatus.values())
                <option value="${s.name()}">${s.name()}</option>
            @endfor
        </select>

        <label for="parentId">Bovenliggende categorie (optioneel)</label>
        <select id="parentId" name="parentId">
            <option value="">— geen parent —</option>
            @for(CategoryResponse parent : parentOptions)
                <option value="${parent.id()}">${parent.name()}</option>
            @endfor
        </select>

        <div class="actions">
            <button type="submit">Aanmaken</button>
            <a class="cancel" href="/categories">Annuleren</a>
        </div>
    </form>
</div>
</body>
</html>
```

## Step 4 — List template (overzicht)

`src/main/jte/categories/list.jte`:

```html
@import org.noteplus.noteplus.dto.response.CategoryResponse
@import java.util.List

@param List<CategoryResponse> categories

<!DOCTYPE html>
<html lang="nl">
<head>
    <meta charset="UTF-8">
    <title>Categorieën — NotePlus</title>
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
            max-width: 700px;
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
        td { padding: 14px 28px; font-size: 14px; color: #1a1a1a; border-bottom: 1px solid #f3f4f6; }
        tr:last-child td { border-bottom: none; }
        .badge {
            display: inline-block;
            padding: 2px 10px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 500;
            background: #e0e7ff;
            color: #3730a3;
        }
        .empty { padding: 40px 28px; text-align: center; color: #9ca3af; font-size: 14px; }
    </style>
</head>
<body>
<div class="card">
    <div class="card-header">
        <h1>Categorieën</h1>
        <a class="btn" href="/categories/new">+ Nieuwe categorie</a>
    </div>

    @if(categories.isEmpty())
        <div class="empty">Nog geen categorieën aangemaakt.</div>
    @else
        <table>
            <thead>
                <tr>
                    <th>Naam</th>
                    <th>Kleur</th>
                    <th>Status</th>
                    <th>Parent</th>
                </tr>
            </thead>
            <tbody>
                @for(CategoryResponse cat : categories)
                    <tr>
                        <td>${cat.name()}</td>
                        <td>
                            @if(cat.color() != null)
                                <span class="badge">${cat.color()}</span>
                            @endif
                        </td>
                        <td>${cat.status() != null ? cat.status() : "—"}</td>
                        <td>${cat.parentName() != null ? cat.parentName() : "—"}</td>
                    </tr>
                @endfor
            </tbody>
        </table>
    @endif
</div>
</body>
</html>
```

## Step 5 — Compile and test

Open in browser:
- http://localhost:8080/categories — list page (should show empty state or seeded categories)
- http://localhost:8080/categories/new — form page
- Fill in name, select color/status, submit — should redirect to list with the new category visible

If the JTE template throws a rendering error, check:
- That `gg.jte.developmentMode=true` is in application.properties
- That all @import statements match the actual package names in your project
- That CategoryColor and CategoryStatus enum names match exactly
